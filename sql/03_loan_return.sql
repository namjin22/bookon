-- 대출·반납 SQL (JDBC PreparedStatement 용, ? 는 바인드 변수)
-- 이 파일의 문장은 src/main/java/com/bookon/dao/LoanDAO.java 와 같은 문장이다. 한쪽을 바꾸면 다른 쪽도 같이 바꾼다.
-- 대출과 반납은 각각 setAutoCommit(false) 한 트랜잭션이다. 실패하면 rollback, 끝까지 성공하면 commit 한다.
-- MAX_LOAN_COUNT = 3 (LoanDAO 상수).
--
-- 잠그는 순서 (교착 방지)
--   대출: 학생 → 도서(재고) → 대출(새 행)
--   반납: 학생 → 대출 → 도서(재고)   (학생을 가장 먼저 잠근다)
-- 반납이 "대출 → 도서 → 학생" 순서로 잠그면 교착(ORA-00060)이 난다.
--   예) 같은 학생 S가 도서 B의 한 권을 반납하는 동시에 B의 다른 권을 대출할 때
--       대출 트랜잭션: 학생 S 행을 잠그고 도서 B 재고 UPDATE를 기다린다.
--       반납 트랜잭션: 대출 행과 도서 B 재고를 잠그고 학생 S 패널티 UPDATE를 기다린다.
--       서로가 가진 행을 기다리므로 영원히 끝나지 않는다.
-- 둘 다 학생 행을 가장 먼저 잠그면 같은 학생의 대출·반납은 한 줄로 차례대로 처리되어 교착이 없다.
-- 다른 학생끼리는 학생 행이 달라서 서로 막지 않는다.

-- [대출 1] 학생 행 잠금 (같은 학생이 동시에 눌러도 한도를 넘지 않게 commit/rollback 까지 잠근다)
-- 파라미터: (1) STUDENT_ID
-- 1행 = 성공 → [대출 2]
-- 0행 = 실패 (없는 학생) → rollback
SELECT STUDENT_ID FROM TBL_STUDENT WHERE STUDENT_ID = ? FOR UPDATE;

-- [대출 2] 학생 상태 읽기 (잠근 뒤에 읽는다)
-- 파라미터: (1) STUDENT_ID
-- 항상 1행. 열 값을 아래 순서대로 보고 먼저 걸린 사유 하나만 실패로 안내한다. 모두 통과하면 성공 → [대출 3]
--   USE_YN = 'N'          → 실패 (이용 중지 학생) → rollback
--   PENALIZED = 1         → 실패 (대출 정지 중, PENALTY_END_DATE 가 다시 빌릴 수 있는 첫 날) → rollback
--   OVERDUE_COUNT > 0     → 실패 (연체 중인 미반납 도서가 있음) → rollback
--   LOAN_COUNT >= 3       → 실패 (최대 대출 권수 도달) → rollback
SELECT s.STUDENT_ID, s.STUDENT_NO, s.STUDENT_NAME, s.USE_YN, s.PENALTY_END_DATE,
       CASE WHEN s.PENALTY_END_DATE > TRUNC(SYSDATE) THEN 1 ELSE 0 END AS PENALIZED,
       (SELECT COUNT(*) FROM TBL_LOAN l
        WHERE l.STUDENT_ID = s.STUDENT_ID AND l.RETURN_YN = 'N') AS LOAN_COUNT,
       (SELECT COUNT(*) FROM TBL_LOAN l
        WHERE l.STUDENT_ID = s.STUDENT_ID AND l.RETURN_YN = 'N'
          AND l.DUE_DATE < TRUNC(SYSDATE)) AS OVERDUE_COUNT
FROM TBL_STUDENT s
WHERE s.STUDENT_ID = ?;

-- [대출 3] 재고 차감
-- 파라미터: (1) BOOK_ID
-- 1행 변경 = 성공 → [대출 4]
-- 0행 = 실패 (재고 없음, 사용중지 도서, 없는 도서) → rollback
UPDATE TBL_BOOK SET AVAIL_QTY = AVAIL_QTY - 1
WHERE BOOK_ID = ? AND USE_YN = 'Y' AND AVAIL_QTY > 0;

-- [대출 4] 대출 등록 (이용 중인 학생일 때만 한 행을 만든다)
-- 파라미터: (1) BOOK_ID  (2) DUE_DATE(반납예정일)  (3) STUDENT_ID
-- 1행 삽입 = 성공 → commit
-- 0행 또는 예외 = 실패 → [대출 3]의 재고 차감까지 rollback
INSERT INTO TBL_LOAN (LOAN_ID, STUDENT_ID, BOOK_ID, LOAN_DATE, DUE_DATE, RETURN_YN)
SELECT SEQ_LOAN.NEXTVAL, s.STUDENT_ID, ?, TRUNC(SYSDATE), ?, 'N'
FROM TBL_STUDENT s
WHERE s.STUDENT_ID = ? AND s.USE_YN = 'Y';

-- [반납 0 준비] 반납할 대출의 학생 찾기 (잠금 대상 학생을 알기 위한 읽기, 아직 잠그지 않는다)
-- 파라미터: (1) LOAN_ID
-- 1행 = 성공 → [반납 0]
-- 0행 = 실패 (없는 대출번호, LoanDAO 의 NOT_FOUND) → 아무것도 바꾸지 않고 '해당 대출 번호를 찾을 수 없습니다' 오류 안내
--        (이미 반납된 건은 이 단계에서는 1행이 나오고, [반납 1]에서 0행으로 구분된다)
SELECT STUDENT_ID FROM TBL_LOAN WHERE LOAN_ID = ?;

-- [반납 0] 학생 행 잠금 (반납의 맨 앞 단계)
-- 이유: 대출이 학생 → 도서 → 대출 순서로 잠그므로 반납도 학생을 먼저 잠가야 교착(ORA-00060)이 나지 않는다.
--       반납이 대출 → 도서 → 학생 순서이면 같은 학생의 동시 대출·반납이 서로를 기다린다 (파일 맨 위 설명).
-- 파라미터: (1) STUDENT_ID  ([반납 0 준비]에서 읽은 값)
-- 1행 = 성공 → [반납 1]
-- 0행 = 실패 (학생 행을 잠글 수 없음) → rollback, 반납 실패 안내
SELECT STUDENT_ID FROM TBL_STUDENT WHERE STUDENT_ID = ? FOR UPDATE;

-- [반납 1] 반납 처리
-- 파라미터: (1) LOAN_ID
-- 1행 변경 = 성공 → [반납 2]
-- 0행 = 실패 (이미 반납된 건) → 재고 복구와 패널티를 하지 않고 '이미 반납 처리된 건' 안내 (msg=alreadyReturned)
UPDATE TBL_LOAN SET RETURN_YN = 'Y', RETURN_DATE = TRUNC(SYSDATE)
WHERE LOAN_ID = ? AND RETURN_YN = 'N';

-- [반납 2] 재고 복구 ([반납 1]이 1행 바뀐 경우에만 실행)
-- 파라미터: (1) LOAN_ID
-- 1행 변경 = 성공 → [반납 3]
-- 0행 또는 예외(ORA-02290: 보유 권수 초과) = 실패 → [반납 1]까지 rollback
UPDATE TBL_BOOK SET AVAIL_QTY = AVAIL_QTY + 1
WHERE BOOK_ID = (SELECT BOOK_ID FROM TBL_LOAN WHERE LOAN_ID = ?);

-- [반납 3] 연체일수 읽기 (반납일 − 반납 예정일, 늦지 않았으면 0)
-- 파라미터: (1) LOAN_ID
-- 항상 1행(숫자 1개). 0 = 연체 아님(예정일 당일 포함) → 패널티 없이 commit (msg=returned)
-- 1 이상 = 연체 반납 → [반납 4] 후 commit (msg=returnedLate&days=연체일수)
SELECT GREATEST(0, TRUNC(RETURN_DATE) - TRUNC(DUE_DATE)) FROM TBL_LOAN WHERE LOAN_ID = ?;

-- [반납 4] 연체 패널티 ([반납 3]의 값이 1 이상일 때만 실행)
-- 새 PENALTY_END_DATE = GREATEST(NVL(기존 값, 오늘), 오늘) + 연체일수. 기존 정지가 남아 있으면 그 날부터, 끝났으면 오늘부터 더한다.
-- 파라미터: (1) 연체일수([반납 3]의 값)  (2) STUDENT_ID
-- 1행 변경 = 성공 → commit
-- 0행 = 실패 → [반납 1]~[반납 2]까지 rollback
UPDATE TBL_STUDENT
SET PENALTY_END_DATE = GREATEST(NVL(PENALTY_END_DATE, TRUNC(SYSDATE)), TRUNC(SYSDATE)) + ?
WHERE STUDENT_ID = ?;
