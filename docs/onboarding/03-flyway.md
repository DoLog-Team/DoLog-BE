# 3. DB 스키마 변경: Flyway

[목차](00-start.md)

## 현재 구조

`src/main/resources/db/migration`의 SQL을 Flyway가 실행하고 Hibernate `ddl-auto=validate`가 엔티티와 대조한다. local도 동일하다.

| 버전 | 내용 |
|---|---|
| V1 | 기존 스키마 baseline |
| V2 | 계정 nullable 필드·소셜 식별자·복수 세션 기반 |
| V3 | 계정 역할 및 세션 제약 |
| V4 | 전시 계정과 코드 로그인 |
| V5 | VER2 공통 스키마 |

local에서만 `db/seed/R__local_seed.sql`을 추가 로드한다. Repeatable 파일은 체크섬이 달라지면 다시 실행되며, 현재 INSERT IGNORE는 기존 데이터 수정용이 아니다.

## 변경 순서

1. 관련 브랜치의 SQL과 대상 DB `flyway_schema_history`를 확인하고 팀이 예약한 번호와 겹치지 않게 정한다. 로컬 최대값만 보고 다음 번호를 확정하지 않는다.
2. `V<새번호>__<설명>.sql`을 추가한다. 적용된 파일은 수정·삭제·번호 변경하지 않는다.
3. 엔티티와 호출부를 함께 수정한다. NOT NULL·UNIQUE·FK 강화는 기존 데이터 이관 순서까지 검토한다.
4. 격리된 MySQL에서 빈 DB 전체 적용과 기존 데이터 업그레이드를 각각 확인한다. 앱의 Hibernate 검증과 관련 테스트도 수행한다.
5. PR에 스키마 변경, 데이터 이관, 검증 결과를 기록한다.

현재 `baseline-on-migrate=true`다. 이력 없는 비어 있지 않은 스키마에 baseline을 기록하는 설정이지 기존 테이블을 V1과 동일하게 고쳐주는 기능이 아니다. 기존 DB에 무심코 적용하지 않는다.

V2~V5에는 데이터에 따라 실패할 수 있는 제약 변경이 있다. 빈 DB 테스트 통과만으로 운영 데이터 호환성을 보장하지 않는다. `repair`, `outOfOrder`, 이력 삭제로 오류를 숨기지 말고 원인을 먼저 확인한다.

## 확인

```sql
SELECT installed_rank, version, description, type, script, checksum, success
FROM flyway_schema_history ORDER BY installed_rank;
```

[DB 조회](06-db-queries.md)로 접속한다. SQL 변경 PR은 `.github/workflows/db-diff.yml`이 격리 DB에서 전후 스키마를 비교한다. ERD/상세 diff는 기존 운영 데이터의 이관 성공을 보장하지 않는다.
