# 계약 (Java ↔ 화면 ↔ DB)

이 문서가 기준이다. 바꾸려면 **단톡에 먼저 알리고** 이 파일을 같이 고친다.

## 1. 화면(JSP) 위치 — `src/main/webapp/`

`index.jsp`(redirect만), `main.jsp`, `book/list.jsp`, `book/form.jsp`, `student/list.jsp`, `student/form.jsp`, `loan/list.jsp`, `css/style.css`, `js/app.js`

## 2. 서블릿 URL

서블릿은 **정확한 URL로만** 매핑한다. `/book/*` 같은 와일드카드는 금지다. (JSP로 forward하면 같은 서블릿이 다시 호출되어 무한 루프가 생긴다)
링크와 form action은 `${pageContext.request.contextPath}/...` 서블릿 URL을 쓴다. `form.jsp` 같은 JSP 파일을 직접 가리키지 않는다.

| URL | 방식 | 입력 | 결과 |
|---|---|---|---|
| `/main` | GET | | request: `totalBookCount`, `availableBookCount`, `loanedCount`, `overdueCount`, `recentLoans` |
| `/book/list` | GET | `keyword` | request: `books` |
| `/book/form` | GET | `id`(수정일 때만) | request: `book`(수정일 때만) |
| `/book/save` | POST | `bookId`(수정일 때만), `title`, `author`, `publisher`, `totalQty` | 성공: redirect `/book/list?msg=saved` 또는 `updated` / 실패: form.jsp로 forward + `errors`, `book`, `totalQtyInput` |
| `/book/delete` | POST | `bookId` | redirect `/book/list?msg=deleted` |
| `/student/list` | GET | `keyword` | request: `students` |
| `/student/form` | GET | `id`(수정일 때만) | request: `student`(수정일 때만) |
| `/student/save` | POST | `studentId`(수정일 때만), `studentNo`, `studentName`, `phone` | 도서와 같은 방식 |
| `/student/delete` | POST | `studentId` | redirect `/student/list?msg=deleted` |
| `/loan/list` | GET | `keyword`, `status`(`ALL`/`UNRETURNED`/`OVERDUE`/`RETURNED`) | request: `loans`, `activeStudents`, `availableBooks` |
| `/loan/borrow` | POST | `studentId`, `bookId`, `dueDate`(yyyy-MM-dd) | 성공: redirect `/loan/list?msg=borrowed` / 실패: forward + `error` |
| `/loan/return` | POST | `loanId` | redirect `/loan/list?msg=returned` 또는 `alreadyReturned` |

## 3. 메시지

- 성공 안내: 쿼리 `msg` = `saved` / `updated` / `deleted` / `borrowed` / `returned` / `alreadyReturned`. 화면이 배너로 보여준다. (`alreadyReturned`는 `.message-info`)
- 실패: request 속성 `error`(String)와 `errors`(Map<String,String>, 키는 입력 name). 화면이 `.message-error`와 각 입력 아래 `.form-error`에 보여준다.
- `totalQty`에 숫자가 아닌 값이 들어와도 입력값을 보여주려면 `totalQtyInput`을 우선 사용한다.

## 4. 모델 필드 (camelCase)

- `Book`: bookId, title, author, publisher, totalQty, availQty, useYn, regDate
- `Student`: studentId, studentNo, studentName, phone, useYn, regDate
- `Loan`: loanId, studentId, bookId, loanDate, dueDate, returnYn, returnDate + 조인용 studentName, bookTitle, studentNo, overdue(boolean)

날짜 필드는 `java.sql.Date`다.

## 5. DB 규칙

- 테이블: `TBL_BOOK`, `TBL_STUDENT`, `TBL_LOAN` / 시퀀스: `SEQ_BOOK`, `SEQ_STUDENT`, `SEQ_LOAN` (Oracle 11g라 IDENTITY, FETCH FIRST, OFFSET은 쓰지 않는다)
- 삭제는 `USE_YN='N'`으로 처리한다. (이력 보존)
- `AVAIL_QTY`는 저장값이므로 대출·반납과 **같은 트랜잭션**에서 조건부 UPDATE로 바꾼다. (예: `AVAIL_QTY > 0 AND USE_YN='Y'`) DAO가 연결을 따로 열면 트랜잭션이 깨지므로, 대출·반납은 하나의 Connection으로 처리한다.
- 반납은 여러 번 눌러도 안전해야 한다: `UPDATE ... WHERE LOAN_ID=? AND RETURN_YN='N'`이 1행일 때만 `AVAIL_QTY`를 +1 한다.
- 연체 기준: `RETURN_YN='N' AND DUE_DATE < TRUNC(SYSDATE)` (예정일 당일은 연체가 아니다)
- 일관성 검사: `sql/02_sample_data.sql` 끝의 `AVAIL_QTY` 검증 쿼리가 0행이어야 한다.

## 6. 입력 길이·형식

| 항목 | 규칙 |
|---|---|
| 도서명 | 필수, 300자 이하 |
| 저자 | 필수, 100자 이하 |
| 출판사 | 선택, 100자 이하 |
| 보유 권수 | 1~999 정수 |
| 학번 | 필수, 숫자 4~10자리 (DB 10자) |
| 학생 이름 | 필수, 30자 이하 |
| 연락처 | 선택, 13자 이하, `010-1234-5678` 형식 |
| 반납 예정일 | 필수, 오늘 이후(오늘 포함) |

화면(JS)이 먼저 막고, **서버가 반드시 다시 검증한다.**

## 7. 환경

- DB: `jdbc:oracle:thin:@localhost:1521:XE`, 드라이버 `oracle.jdbc.OracleDriver`, 접속 정보는 `db.properties`(커밋 금지)
- 확정 환경: **JDK 21 + Tomcat 10.1 + `jakarta.servlet`** (3번 PC에서 실제 Tomcat·Oracle로 검증한 조합). 전원 노트북을 이 버전에 맞춘다.
- JSP에서 JSTL을 쓸 때: `<%@ taglib prefix="c" uri="jakarta.tags.core" %>` (예전 `http://java.sun.com/jsp/jstl/core` 주소는 쓰지 않는다)
  `WEB-INF/lib`에 Jakarta JSTL 3.0의 API jar와 구현 jar 두 개가 모두 필요하다. (화면 담당이 넣는다)
- 제출용 소스에도 이 환경을 README에 적어 둔다. (선생님이 열어볼 때 버전이 달라 보이지 않게)

## 8. v2 추가 규칙 (2026-11-16) — 위 내용과 다르면 이 절이 우선한다

### 8-1. 학생당 최대 대출 권수
- 한 학생이 동시에 빌릴 수 있는 **미반납 도서는 최대 3권**이다. 상수 `MAX_LOAN_COUNT = 3`은 `LoanDAO`에 한 곳에만 두고, 서블릿이 `maxLoanCount`(int) request 속성으로 화면에 넘긴다. (JSP에 3을 직접 쓰지 않는다)
- 대출 처리에서 학생 행을 `SELECT ... FOR UPDATE`로 먼저 잠근 뒤 미반납 권수를 센다. 같은 학생이 동시에 눌러도 한도를 넘지 않게 하기 위해서다.

### 8-2. 연체 패널티 (대출 정지)
- `TBL_STUDENT.PENALTY_END_DATE DATE`(NULL 가능) = 대출을 다시 할 수 있는 **첫 날**. `PENALTY_END_DATE > TRUNC(SYSDATE)`이면 대출 정지 중이다.
- 연체한 책을 반납하면(반납일 > 반납 예정일) 연체일수만큼 정지한다. `새 값 = GREATEST(NVL(기존 값, 오늘), 오늘) + 연체일수`, 연체일수 = 반납일 − 반납 예정일. 정지는 누적된다. 반납 UPDATE·재고 복구와 **같은 트랜잭션**에서 처리한다.
- 대출 조건은 아래 순서로, **한 트랜잭션** 안에서 확인한다. 실패 사유마다 다른 `error` 문구를 보여준다.
  1. 없는 학생이거나 이용 중지(`USE_YN='N'`) 학생
  2. 대출 정지 중
  3. 연체 중인 미반납 도서가 있음
  4. 미반납 권수가 최대 권수 이상
  5. 재고 없음 또는 이용 중지 도서
- 반납 결과 메시지: 연체였으면 `msg=returnedLate&days=연체일수`, 아니면 `msg=returned`, 이미 반납이면 `msg=alreadyReturned`.
- `Student` 모델에 `penaltyEndDate`(Date), `penalized`(boolean, SQL에서 계산), `loanCount`(int, 미반납 권수), `overdueCount`(int, 연체 중인 미반납 권수)를 추가한다.
- 학생 상태 표시 우선순위: 이용 안 함 > 대출 정지 > 연체 중 > 이용 가능. `/student/list`와 `/loan/list`는 `maxLoanCount`를 함께 넘긴다.

### 8-3. 도서 코드 (BOOK_ID = 십진분류 코드)
- `BOOK_ID`는 시퀀스 숫자가 아니라 **도서 코드 `NNN-NNN`** (문자열)이다. 앞 3자리 = 한국십진분류(KDC) 분류번호, 뒤 3자리 = 같은 분류 안의 일련번호. 예: `813-001`.
- DB: `BOOK_ID VARCHAR2(7)`, `CHECK (REGEXP_LIKE(BOOK_ID, '^[0-9]{3}-[0-9]{3}$'))`. `TBL_LOAN.BOOK_ID`(FK)도 같은 타입. `SEQ_BOOK`는 없앤다.
- 등록할 때 사용자가 분류번호 3자리(`kdc`)를 입력하고, DAO가 같은 분류의 최대 일련번호 + 1로 코드를 만든다. 삭제(`USE_YN='N'`)한 도서의 번호도 포함해서 세므로 번호를 재사용하지 않는다. 같은 코드가 동시에 만들어져 PK 중복(ORA-00001)이 나면 최대 3번 다시 시도한다.
- 코드는 한 번 정해지면 바뀌지 않는다. 수정 화면에서는 읽기 전용으로만 보여준다.
- 모델: `Book.bookId`, `Loan.bookId`는 `String`. `/book/save` 입력에 `kdc`(등록일 때만, 숫자 3자리 필수) 추가, 오류는 `errors.kdc`. 도서 코드를 받는 파라미터(`bookId`, `id`)는 `[0-9]{3}-[0-9]{3}` 형식만 받는다. JSP에서 수정 모드 판단은 `not empty book.bookId`.
- 샘플 데이터의 분류번호는 AI가 준 값을 그대로 믿지 말고 국립중앙도서관 등 서지 정보로 직접 확인한다. (HITL 기록 대상)
