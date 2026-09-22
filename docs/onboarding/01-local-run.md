# 1. 로컬 실행

[목차](00-start.md)

## 준비 및 기동

JDK 17과 Docker Compose가 필요하다. 저장소 루트에서 실행한다.

```bash
java -version
docker compose up -d
docker compose ps
./gradlew bootRun
```

MySQL이 healthy인지 확인한다. 앱은 컨테이너가 아닌 로컬 JVM에서 실행한다. `bootRun`은 `ACTIVE`가 없으면 local을 주입한다. IntelliJ에서 `ServerApplication`을 직접 실행할 때는 실행 구성에 `ACTIVE=local`을 지정한다. JAR 직접 실행의 기본 프로파일은 prod다.

| 대상 | 주소 |
|---|---|
| API | http://localhost:8080/api |
| Swagger | http://localhost:8080/api/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/api/v3/api-docs |
| Actuator | http://localhost:9090/actuator/health |
| MySQL | localhost:3307 / DB dolog / 사용자 dolog / 암호 localpassword |
| LocalStack S3 | http://localhost:4567 / 버킷 dolog-local |

```bash
curl -fsS http://localhost:9090/actuator/health
curl -fsS http://localhost:8080/api/exhibitions
```

## 설정과 시드

`src/main/resources/application-local.properties`가 로컬 전용 더미 설정을 공급한다. 별도 `.env`는 필요 없다. 셸 환경변수가 파일보다 우선하므로 기존 `ACTIVE`, `DATABASE_URL` 등이 다른 환경을 가리키는지 확인한다. 로컬 테스트에 운영 자격증명을 사용하지 않는다.

Flyway V1~V5 적용 후 `db/seed/R__local_seed.sql`을 실행한다. 전시 `local-demo`, 작가·프로필·작품·구역 등을 만든다. 고정 ID와 `INSERT IGNORE`를 사용하므로 재실행이 기존 행을 갱신하지는 않는다. 전시 날짜는 2026-09-01~09-30으로 고정되어 있다.

| 역할 | 로컬 사용 방법 |
|---|---|
| DOLOG_ADMIN | admin@local.test / admin1234, AdminInitializer가 생성 |
| EXHIBITION_ADMIN | 시드 계정 test@test.com 존재. 현재 로그인 제한은 아래 참고 |
| ARTIST_ADMIN | 관리자 토큰으로 개발용 작가 로그인, fixture ARTIST_1 |

현재 `/auth/login`은 DOLOG_ADMIN만 허용한다. 시드 전시 코드 `LOCAL001`은 전시 로그인 검증에서 금지하는 문자(0, 1, L, O)를 포함하므로 그대로 로그인할 수 없다. 전시 역할 테스트에는 유효한 코드와 계정·전시 상태를 가진 별도 로컬 데이터가 필요하다. 이번 문서 작업에서는 시드나 인증 코드를 변경하지 않았다.

로그인 요청은 [Postman 가이드](07-postman.md)를 따른다. 시드 이미지는 외부 placeholder이며 LocalStack 업로드 검증을 대체하지 않는다.

## 테스트 및 S3

```bash
./gradlew test
./gradlew build
```

기존 Spring 통합 테스트는 local 프로파일과 실제 DB를 사용한다. 먼저 로컬 Compose를 기동한다. 보고서는 `build/reports/tests/test/index.html`이다.

앱 시작 시 S3Config가 로컬 버킷 생성을 시도한다. 실패를 경고 로그로 남기고 기동을 계속할 수 있어 업로드는 별도로 확인해야 한다.

```bash
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=ap-northeast-2 \
  aws --endpoint-url=http://localhost:4567 s3 ls s3://dolog-local/ --recursive
```

## 종료와 초기화

`docker compose down`은 볼륨을 보존한다. 로컬 데이터를 전부 버리고 초기 상태로 돌아갈 때만 `docker compose down -v` 후 다시 기동한다. DB와 LocalStack 볼륨이 함께 삭제된다.
