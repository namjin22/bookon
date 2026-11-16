-- 대출·반납 SQL (JDBC PreparedStatement 용, ? 는 바인드 변수)
-- 대출과 반납은 각각 setAutoCommit(false) 한 트랜잭션이다. 실패하면 rollback, 끝까지 성공하면 commit 한다.
-- MAX_LOAN_COUNT = 3 (LoanDAO 상수). 대출 확인은 아래 번호 순서대로 하고, 먼저 걸린 사유 하나만 안내한다.

-- [대출 1] 학생 확인 + 학생 행 잠금 (같은 학생이 동시에 눌러도 한도를 넘지 않게 commit/rollback 까지 잠근다)
-- 파라미터: (1) STUDENT_ID
-- 0행 = 실패 (사유 1: 없는 학생) → rollback
-- 1행이고 USE_YN = 'N' = 실패 (사유 1: 이용 중지 학생) → rollback
-- 1행이고 PENALIZED = 'Y' = 실패 (사유 2: 대출 정지 중, PENALTY_END_DATE 가 다시 빌릴 수 있는 첫 날) → rollback
-- 1행이고 USE_YN = 'Y', PENALIZED = 'N' = 성공 → [대출 2]
SELECT USE_YN, PENALTY_END_DATE,
       CASE WHEN PENALTY_END_DATE > TRUNC(SYSDATE) THEN 'Y' ELSE 'N' END AS PENALIZED
FROM TBL_STUDENT
WHERE STUDENT_ID = ?
FOR UPDATE;

-- [대출 2] 연체 중인 미반납 도서 수
-- 파라미터: (1) STUDENT_ID
-- 결과는 항상 1행(숫자 1개). 0 = 성공 → [대출 3]
-- 1 이상 = 실패 (사유 3: 연체 중인 미반납 도서가 있음) → rollback
SELECT COUNT(*)
FROM TBL_LOAN
WHERE STUDENT_ID = ? AND RETURN_YN = 'N' AND DUE_DATE < TRUNC(SYSDATE);

-- [대출 3] 미반납 권수
-- 파라미터: (1) STUDENT_ID
-- 결과는 항상 1행(숫자 1개). MAX_LOAN_COUNT(3) 미만 = 성공 → [대출 4]
-- MAX_LOAN_COUNT 이상 = 실패 (사유 4: 최대 대출 권수 도달) → rollback
SELECT COUNT(*)
FROM TBL_LOAN
WHERE STUDENT_ID = ? AND RETURN_YN = 'N';

-- [대출 4] 재고 차감
-- 파라미터: (1) BOOK_ID
-- 1행 변경 = 성공 → [대출 5]
-- 0행 = 실패 (사유 5: 재고 없음, 사용중지 도서, 없는 도서) → rollback
UPDATE TBL_BOOK SET AVAIL_QTY = AVAIL_QTY - 1
WHERE BOOK_ID = ? AND USE_YN = 'Y' AND AVAIL_QTY > 0;

-- [대출 5] 대출 등록
-- 파라미터: (1) STUDENT_ID  (2) BOOK_ID  (3) DUE_DATE(반납예정일)
-- 1행 삽입 = 성공 → commit
-- 0행 또는 예외 = 실패 → [대출 4]의 재고 차감까지 rollback
INSERT INTO TBL_LOAN (LOAN_ID, STUDENT_ID, BOOK_ID, LOAN_DATE, DUE_DATE, RETURN_YN)
VALUES (SEQ_LOAN.NEXTVAL, ?, ?, TRUNC(SYSDATE), ?, 'N');

-- [반납 1] 반납 처리
-- 파라미터: (1) LOAN_ID
-- 1행 변경 = 성공 → [반납 2]
-- 0행 = 실패 (이미 반납된 건, 없는 대출번호) → 재고 복구와 패널티를 하지 않고 '이미 반납 처리된 건' 안내 (msg=alreadyReturned)
UPDATE TBL_LOAN SET RETURN_YN = 'Y', RETURN_DATE = TRUNC(SYSDATE)
WHERE LOAN_ID = ? AND RETURN_YN = 'N';

-- [반납 2] 재고 복구 ([반납 1]이 1행 바뀐 경우에만 실행)
-- 파라미터: (1) LOAN_ID
-- 1행 변경 = 성공 → [반납 3]
-- 0행 또는 예외(ORA-02290: 보유 권수 초과) = 실패 → [반납 1]까지 rollback
UPDATE TBL_BOOK SET AVAIL_QTY = AVAIL_QTY + 1
WHERE BOOK_ID = (SELECT BOOK_ID FROM TBL_LOAN WHERE LOAN_ID = ?);

-- [반납 3] 연체 패널티 (반납일 = 오늘 > 반납 예정일 일 때만 학생 행이 바뀐다)
-- 새 PENALTY_END_DATE = GREATEST(NVL(기존 값, 오늘), 오늘) + 연체일수, 연체일수 = 오늘 - 반납 예정일
-- 파라미터: (1)(2)(3) 모두 같은 LOAN_ID
-- 1행 변경 = 연체 반납 → 패널티 부여됨 → [반납 4] 로 연체일수를 읽고 commit (msg=returnedLate&days=연체일수)
-- 0행 = 연체 아님(예정일 당일 포함) → 패널티 없이 commit (msg=returned)
UPDATE TBL_STUDENT s
SET s.PENALTY_END_DATE = GREATEST(NVL(s.PENALTY_END_DATE, TRUNC(SYSDATE)), TRUNC(SYSDATE))
                         + (TRUNC(SYSDATE) - (SELECT TRUNC(l.DUE_DATE) FROM TBL_LOAN l WHERE l.LOAN_ID = ?))
WHERE s.STUDENT_ID = (SELECT l.STUDENT_ID FROM TBL_LOAN l WHERE l.LOAN_ID = ?)
  AND TRUNC(SYSDATE) > (SELECT TRUNC(l.DUE_DATE) FROM TBL_LOAN l WHERE l.LOAN_ID = ?);

-- [반납 4] 연체일수 읽기 ([반납 3]이 1행 바뀐 경우에만 실행, 화면 안내용)
-- 파라미터: (1) LOAN_ID
-- 결과는 항상 1행(숫자 1개, 1 이상) → commit
SELECT TRUNC(SYSDATE) - TRUNC(DUE_DATE)
FROM TBL_LOAN
WHERE LOAN_ID = ?;
