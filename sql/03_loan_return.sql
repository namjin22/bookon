-- 대출·반납 SQL 4개 (JDBC PreparedStatement 용, ? 는 바인드 변수)
-- 하나의 트랜잭션(setAutoCommit(false))으로 묶고, executeUpdate() 반환값(바뀐 행 수)으로 commit/rollback 결정

-- [대출 1] 재고 차감
-- 파라미터: (1) BOOK_ID
-- 1행 변경 = 성공 → 다음 단계(대출 등록) 진행
-- 0행 = 실패 (재고 0, 사용중지 도서, 없는 도서) → rollback 후 '대출 불가' 안내
UPDATE TBL_BOOK SET AVAIL_QTY = AVAIL_QTY - 1
WHERE BOOK_ID = ? AND USE_YN = 'Y' AND AVAIL_QTY > 0;

-- [대출 2] 대출 등록 (활성 학생일 때만)
-- 파라미터: (1) BOOK_ID  (2) DUE_DATE(반납예정일)  (3) STUDENT_ID
-- 1행 삽입 = 성공 → commit
-- 0행 = 실패 (이용중지 학생, 없는 학생) → [대출 1]의 재고 차감까지 rollback 후 '대출 불가' 안내
INSERT INTO TBL_LOAN (LOAN_ID, STUDENT_ID, BOOK_ID, LOAN_DATE, DUE_DATE, RETURN_YN)
SELECT SEQ_LOAN.NEXTVAL, s.STUDENT_ID, ?, TRUNC(SYSDATE), ?, 'N'
FROM TBL_STUDENT s
WHERE s.STUDENT_ID = ? AND s.USE_YN = 'Y';

-- [반납 1] 반납 처리
-- 파라미터: (1) LOAN_ID
-- 1행 변경 = 성공 → 다음 단계(재고 복구) 진행
-- 0행 = 실패 (이미 반납된 건, 없는 대출번호) → 재고 복구를 하지 않고 '이미 반납 처리된 건' 안내 (rollback 또는 변경 없음)
UPDATE TBL_LOAN SET RETURN_YN = 'Y', RETURN_DATE = TRUNC(SYSDATE)
WHERE LOAN_ID = ? AND RETURN_YN = 'N';

-- [반납 2] 재고 복구 ([반납 1]이 1행 바뀐 경우에만 실행)
-- 파라미터: (1) LOAN_ID
-- 1행 변경 = 성공 → commit
-- 0행 또는 예외(ORA-02290: 보유 권수 초과) = 실패 → [반납 1]까지 rollback
UPDATE TBL_BOOK SET AVAIL_QTY = AVAIL_QTY + 1
WHERE BOOK_ID = (SELECT BOOK_ID FROM TBL_LOAN WHERE LOAN_ID = ?);
