# BookOn — 도서 대출 관리

응용프로그래밍 개발 2학기 팀 프로젝트. 도서·학생·대출·반납을 관리하는 웹 애플리케이션.

- 스택: Eclipse Dynamic Web Project, Tomcat, Java, JSP/Servlet, JDBC, Oracle 11g XE, HTML/CSS/JS
- Tomcat/JDK 버전: **수업 PC 기준으로 확인 후 확정** (확정되면 이 줄을 고친다)
- 계약(URL·파라미터·필드명·규칙)은 [docs/contract.md](docs/contract.md)가 기준이다.

## 팀과 담당 폴더

| 역할 | GitHub | 담당 폴더 |
|---|---|---|
| 팀장·통합 | namjin22 | `docs/`, `README.md`, 최종 통합·발표 |
| DB (1번) | yangeunjun00 | `sql/` |
| 화면 (2번) | Beom09 | `src/main/webapp/` (JSP, css, js) |
| Java·Servlet (3번) | sxonz | `src/main/java/` |

**자기 폴더만 수정한다.** 남의 폴더를 고쳐야 하면 단톡에 먼저 말한다.

## 작업 규칙 (팀장 승인 없이 바로 push 해도 된다)

1. 작업 시작 전: `git pull --rebase`
2. 내 폴더의 파일만 고친다.
3. 작게 자주 커밋하고 `main`에 바로 push 한다. 커밋 메시지는 `[db]`, `[view]`, `[java]`, `[docs]`로 시작한다. 예: `[view] 도서 목록 JSP 변환`
4. push가 거부되면 `git pull --rebase` 후 다시 push 한다.
5. **컬럼명, 값(Y/N), URL, 파라미터 이름을 바꿀 때는 먼저 단톡에 알리고 `docs/contract.md`를 같이 고친다.** 한 곳만 바뀌면 통합 때 깨진다.
6. `db.properties`와 비밀번호는 커밋하지 않는다. (`.gitignore`에 들어 있다)
7. AI가 만든 코드는 직접 실행해서 확인한 뒤 커밋하고, 확인 기록은 `docs/hitl/`에 남긴다.
8. 깨진 상태로 push 하지 않는다. 자기 화면·기능이 실행되는지 확인하고 올린다.

## 로컬 환경 세팅

1. Oracle 11g XE를 설치한다. 각자 자기 PC의 DB를 쓴다.
2. `sql/`의 스크립트를 순서대로 실행한다: `00_create_user.sql`(system 계정) → `01_create_tables.sql` → `02_sample_data.sql`
3. `src/main/java/db.properties.example`을 `db.properties`로 복사하고 접속 정보를 넣는다. (커밋하지 않는다)
4. Eclipse에서 이 레포를 가져온다. (프로젝트 뼈대가 올라오면 안내를 추가한다)

DB 접속 주소: `jdbc:oracle:thin:@localhost:1521:XE`

## 폴더 구조

```
sql/                  DB 스크립트 (1번)
src/main/java/        Java: model, dao, servlet, filter, util (3번)
src/main/webapp/      JSP, css, js, WEB-INF/lib (2번)
docs/                 계약, HITL 기록, 발표·제출 자료 (팀장)
```
