# 6. DB 데이터 확인

[목차](00-start.md)

## 접속

DataGrip/IntelliJ Database에서 MySQL, localhost:3307, DB `dolog`, 사용자 `dolog`, 암호 `localpassword`를 사용한다. 모두 로컬 전용 값이다.

```bash
docker compose exec mysql mysql -u dolog -p dolog
```

암호는 프롬프트에 입력한다. 운영/dev DB는 공개 포트를 열지 않고 권한이 있는 SSH 터널을 사용한다.

## 조회 예시

UUID는 BINARY(16)이므로 사람이 읽을 때 `BIN_TO_UUID`, 조건에 넣을 때 `UUID_TO_BIN`을 사용한다.

```sql
SHOW TABLES;

SELECT BIN_TO_UUID(id) AS id, role, account_status
FROM accounts;

SELECT BIN_TO_UUID(id) AS id, slug, is_public
FROM exhibitions;

SELECT BIN_TO_UUID(id) AS id, title, status, BIN_TO_UUID(exhibition_id) AS exhibition_id
FROM artworks
WHERE exhibition_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444');

SELECT version, description, success
FROM flyway_schema_history ORDER BY installed_rank;

SHOW CREATE TABLE accounts;
SHOW CREATE TABLE artworks;
```

비밀번호 해시나 Refresh Token 원문을 조회·공유할 필요는 없다. 고정 시드 ID와 관계는 `src/main/resources/db/seed/R__local_seed.sql`을 참고한다.

## 데이터 변경

직접 데이터 편집은 로컬에서만 한다. 스키마 변경은 SQL 콘솔에서 끝내지 않고 Flyway 파일로 관리한다. 앱이 바꾼 값이 안 보이면 조회 새로고침과 트랜잭션 종료 여부를 확인한다.

초기화가 필요하면 [로컬 실행](01-local-run.md)의 볼륨 삭제 범위를 확인한다. 운영 백업을 dev에 가져오는 스크립트는 dev DB를 삭제·재생성하므로 일반 조회 도구로 사용하지 않는다.
