# HITL 기록 — yangeunjun00 / DB (1번)

이 파일을 `docs/hitl/이름.md`로 복사해서 쓴다. 평가에서 AI 코드를 사람이 검증했다는 증거로 쓴다.

## 담당 기능

- Oracle 11g XE 프로젝트 전용 계정 `bookon` 생성
- 테이블·시퀀스·제약조건 생성 (`TBL_BOOK`, `TBL_STUDENT`, `TBL_LOAN` / `SEQ_BOOK`, `SEQ_STUDENT`, `SEQ_LOAN`)
- 샘플 데이터 입력 (학생 8명, 도서 18권, 대출 10건)
- 대출·반납 SQL (`03_loan_return.sql`) — 실행 확인 후 push 예정

## 작성 코드

| 파일 | 내용 | AI가 만든 부분 | 내가 직접 확인·수정한 부분 |
|---|---|---|---|
| `sql/00_create_user.sql` | `bookon` 계정 생성, 권한 부여 (system 계정으로 실행) | CREATE USER / GRANT 문. 비밀번호는 코드에 넣지 않고 실행 시 `&bookon_pw`로 입력받게 구성 | 팀 규칙대로 비밀번호를 레포에서 제외. `sqlplus / as sysdba`로 직접 실행해 계정 생성 확인 |
| `sql/01_create_tables.sql` | 테이블 3개, 시퀀스 3개, PK/FK/CHECK/UNIQUE, 인덱스 | 전체 DDL 초안 | 컬럼·도메인을 설계서와 맞춰 확인, 글자 수 기준(CHAR) 반영 요청, 주석·구분선 제거 요청, 직접 실행해 통과 확인 |
| `sql/02_sample_data.sql` | 샘플 데이터 INSERT, 시퀀스 재생성, 검증 쿼리 | INSERT 문 전체, 검증 쿼리 | 팀장 검증 쿼리가 내 테이블 구조와 맞는지 확인 요청, 직접 실행해 결과 확인 |
| `sql/03_loan_return.sql` | 대출(재고 차감 + 대출 기록), 반납(반납 처리 + 재고 복구) | 4개 SQL 전체 | 아직 push 전. SQL*Plus에서 직접 실행해 확인한 뒤 올린다 |

## 테스트 기록

| 날짜 | 무엇을 | 어떻게(직접 실행) | 결과 | 캡처/증거 |
|---|---|---|---|---|
| 2026-11-09 | `00_create_user.sql` | `sqlplus / as sysdba` | 계정 생성 확인 (`SELECT username FROM dba_users` 결과 확인) | (캡처 첨부) |
| 2026-11-09 | `01_create_tables.sql` | `bookon` 계정으로 SQL*Plus 실행 | 통과 | (캡처 첨부) |
| 2026-11-09 | `02_sample_data.sql` (첫 버전) | `bookon` 계정으로 SQL*Plus 실행 | 통과 | (캡처 첨부) |
| 2026-11-09 | 최신 01 재실행 후 `02_sample_data.sql` | cmd의 SQL*Plus에서 UTF-8 파일 실행 | 한글 깨짐, ORA-01756 / ORA-00917 / ORA-01400 발생 | (로그 첨부) |
| | `02_sample_data.sql` 재실행 후 건수 확인 | cp949 인코딩으로 다시 실행 | (실행 후 기록) | (캡처 첨부) |
| | `03_loan_return.sql` 시나리오 7개 (재고 0, 비활성 도서, 비활성 학생, 정상 대출, 정상 반납, 중복 반납, 재고 일관성) | SQL*Plus에서 직접 실행 | (실행 후 기록) | (캡처 첨부) |

## AI 오류와 수정

| 날짜 | AI가 낸 오류 | 어떻게 발견했나 | 어떻게 고쳤나 |
|---|---|---|---|
| 2026-11-09 | `01_create_tables.sql` 첫 버전이 문자열 컬럼 길이를 바이트 기준 `VARCHAR2(n)`으로 만들어 한글 글자 수 기준(요구사항 4번)을 빠뜨림 | 새 요구사항과 대조하다 발견 | `VARCHAR2(n CHAR)`로 전부 수정 |
| 2026-11-09 | `02_sample_data.sql`을 UTF-8로만 주고, 한국어 Windows cmd의 SQL*Plus가 cp949로 읽는다는 점을 미리 알리지 않음 | 실행 로그에서 한글이 깨지고 ORA-01756 / 00917 / 01400 발생 | 로그의 깨진 글자가 UTF-8 바이트를 cp949로 읽은 결과와 일치하는 것을 확인. cp949 사본으로 실행하거나, `chcp 65001` + `NLS_LANG=KOREAN_KOREA.AL32UTF8` 설정 안내. 레포의 파일은 UTF-8 유지 |

## 발표 역할

- (발표에서 맡을 부분)
