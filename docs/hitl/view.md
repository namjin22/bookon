# HITL 기록 — (Beom09 / 화면 2번)

이 파일을 `docs/hitl/이름.md`로 복사해서 쓴다. 평가에서 AI 코드를 사람이 검증했다는 증거로 쓴다.

## 담당 기능

- 화면(JSP, css, js): `src/main/webapp/`
- 이번 범위: 도서·학생·대출·대시보드 JSP, 공통 헤더 include, Jakarta JSTL 3.0 jar

## 작성 코드

| 파일 | 내용 | AI가 만든 부분 | 내가 직접 확인·수정한 부분 |
|---|---|---|---|
| `book/list.jsp` | 도서 목록, 검색, msg 배너, 삭제 버튼 | HTML → JSP 변환 전체 (`c:forEach books`, `c:out` 출력, contextPath 링크) | 빈 목록 문구, `?msg=saved` 배너, XSS 입력 이스케이프를 브라우저에서 확인 |
| `book/form.jsp` | 도서 등록·수정 폼 | 변환 전체 (`book`/`totalQtyInput` 값 유지, `bookId` 있으면 수정 모드, `errors.입력이름` 표시) | 등록 폼 표시 확인, 출판사 선택 항목(`*`·required 없음) 확인 |
| `student/list.jsp`, `student/form.jsp` | 학생 목록·등록·수정 | book과 같은 규칙으로 변환 (`students`, `student`, `errors`) | 테스트 데이터로 목록·수정·검증 실패 화면 확인 |
| `loan/list.jsp` | 대출 등록·목록·반납 | 변환 (`loans`, `activeStudents`, `availableBooks`, 상태 배지, msg 3종) | 상태별 배지·반납 버튼, 실패 시 선택값 유지 확인 |
| `main.jsp` | 대시보드 | 변환 (통계 4개, `recentLoans`) | 숫자 쉼표, 빈 데이터 처리 확인 |
| `WEB-INF/include/header.jspf` | 공통 헤더(메뉴) | 헤더 분리, `activeMenu`로 현재 메뉴 표시 | 도서 메뉴가 활성으로 표시되는지 확인 |
| `css/style.css`, `js/app.js` | 공통 스타일·검증 | 모바일 표·검색창, maxlength, 반납 예정일 검증 | 390px 화면 확인, 반납 예정일 과거 날짜 차단 확인 |
| `WEB-INF/lib/*.jar` | JSTL API 3.0.2 + 구현 3.0.1 | Maven Central에서 받음 | SHA-1 체크섬 일치 확인 |

## 테스트 기록

환경: JDK 21.0.10 + Tomcat 10.1.52 (포트 8081, 8080은 Oracle XE가 사용 중), 컨텍스트 `/bookon`. 서블릿 소스(`src/main/java`)를 컴파일해 함께 배포했고 `db.properties`는 없는 상태.

### v2 연동 상태 (2026-11-23)

이번 확인에서는 로컬 Oracle XE가 설치되어 있지 않아 `01_create_tables.sql`/`02_sample_data.sql` 실행 및 실제 서블릿 화면 캡처를 수행하지 않았다. 따라서 아래 항목은 **소스 대조 결과**이며, 직접 실행 검증 결과로 기록하지 않는다. 실제 DB 검증은 Oracle 접속 가능한 팀원 환경에서 다시 수행한다.

| 항목 | 소스 대조 결과 | 실제 실행 검증 |
|---|---|---|
| 도서 코드 | `book/list.jsp`는 문자열 `bookId`를 URL·폼 값으로 사용한다. `book/form.jsp`의 수정 모드 판단은 v1 숫자 비교(`book.bookId > 0`)이고 등록용 `kdc` 입력이 없어 v2 계약에 맞게 수정이 필요하다. | 대기 |
| 학생 상태 | `student/list.jsp`에 이용 안 함·대출 정지·연체 중·이용 가능 상태 우선순위 표시가 아직 없다. | 대기 |
| 대출 규칙 안내 | `loan/list.jsp`는 기존 `borrowed`/`returned`/`alreadyReturned` 배너와 오류 영역을 표시한다. `returnedLate` 및 `days` 배너, `maxLoanCount`/학생 상태 표시는 추가가 필요하다. | 대기 |
| 대시보드 | `main.jsp`는 `totalBookCount`, `availableBookCount`, `loanedCount`, `overdueCount`, `recentLoans` 계약 필드를 사용한다. | 대기 |

로컬 전용 `src/main/java/db.properties`에는 `bookon` 계정을 설정했으며 `.gitignore` 적용 상태를 확인했다. 비밀번호와 설정 파일은 커밋하지 않는다.

| 날짜 | 무엇을 | 어떻게(직접 실행) | 결과 | 캡처/증거 |
|---|---|---|---|---|
| 2026-11-16 | 도서 목록 JSP 직접 열기 | `/bookon/book/list.jsp` | 200, 헤더·스타일 정상, "등록된 도서가 없습니다." 표시 | `view/view-01-book-list.png` |
| 2026-11-16 | 성공 배너 | `/bookon/book/list.jsp?msg=saved` | 초록 배너 "도서가 등록되었습니다." 표시 | `view/view-01-book-list.png` |
| 2026-11-16 | 도서 등록 폼 | `/bookon/book/form.jsp` | 200, 등록 폼 정상, 출판사에 `*` 없음 | `view/view-02-book-form.png` |
| 2026-11-16 | XSS 방지 | `/bookon/book/list.jsp?keyword=<script>alert(1)</script>` | alert 안 뜸, 검색창에 글자 그대로 표시, HTML에는 `&lt;script&gt;`로 출력 | `view/view-03-xss.png` |
| 2026-11-16 | 서블릿 경로 | `/bookon/book/list` (db.properties 없음) | 500 아님, "도서 목록을 불러오지 못했습니다." 배너 표시 | Tomcat 로그 `클래스 경로에 db.properties가 없습니다.` |
| 2026-11-16 | 학생 목록·폼 JSP | 테스트용 JSP에서 `Student` 모델 객체를 넣고 forward | 목록·빈 목록·`msg=updated` 배너, 수정 모드(hidden studentId), 검증 실패(.form-error, 입력값 유지) 정상, 이름의 `<b>`·`<script>` 이스케이프 | — |
| 2026-11-16 | 대출 목록 JSP | `Loan`/`Student`/`Book` 객체로 테스트 | 연체·대출 중·반납 완료 배지, 반납 버튼/처리 완료, `alreadyReturned` 파란 배너(.message-info), status 선택 유지, 대출 실패 시 학생·도서·반납 예정일 선택 유지 | — |
| 2026-11-16 | 대시보드 JSP | 통계 4개 + `recentLoans` 테스트 | 숫자 `1,248` 형식, 데이터 없을 때 0과 안내 문구 | — |
| 2026-11-16 | 모바일 390px | 학생 목록·대출 목록·대시보드·학생 폼 | 가로 넘침 없음, 상태·관리 열 표시, 검색창 높이 44px | — |
| 2026-11-09 | 테스트 데이터로 JSP 렌더링 | 테스트용 JSP에서 books/book/errors/totalQtyInput을 넣고 forward | 목록·빈 목록·등록·수정(hidden bookId)·검증 실패(.form-error) 모두 정상 | — |

## AI 오류와 수정

| 날짜 | AI가 낸 오류 | 어떻게 발견했나 | 어떻게 고쳤나 |
|---|---|---|---|
| 2026-11-09 | 390px에서 도서 목록 표가 18px 넘쳐 '관리' 열이 잘림 | 브라우저에서 표 너비를 재서 확인 | 모바일에서만 셀·작은 버튼 여백을 줄임 |
| 2026-11-09 | 띄어쓰기 없는 긴 제목이 있으면 모바일 표가 43px 넘침 | XSS 테스트 데이터(`<script>...`)를 넣고 390px에서 확인 | 모바일 표 셀에 `overflow-wrap:anywhere` 추가 |
| 2026-11-16 | 대시보드 숫자가 `1,248`이 아니라 `1248`로 출력됨 (`fmt:formatNumber`는 요청에 언어 정보가 없으면 쉼표 없이 출력) | 렌더링 결과 HTML을 직접 확인 | `main.jsp`에 `<fmt:setLocale value="ko_KR"/>` 추가 |
| 2026-11-09 | 좁은 화면 캡처가 실제 390px 레이아웃이 아니었음 (헤드리스 Chrome 최소 폭 때문에 오른쪽이 잘림) | 캡처 이미지를 직접 보고 발견 | 390px 폭 iframe으로 다시 캡처 |

## 발표 역할

- (작성 예정)
