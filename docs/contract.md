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
- Tomcat/JDK: 수업 PC 기준으로 확인 후 확정. 현재 3번 PC는 Tomcat 10.1.52 + JDK 21 + `jakarta.servlet`.
  Tomcat 9면 `javax.servlet`과 JSTL 1.2, Tomcat 10 이상이면 `jakarta.servlet`과 Jakarta JSTL을 쓴다.
