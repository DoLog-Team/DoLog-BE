# DoLog-BE

DoLog 백엔드 서버 (Spring Boot 3.5 / Java 17 / Gradle)

전체 가이드는 [온보딩 문서](docs/onboarding/00-start.md)를 참고하세요.

## 개발 환경 실행방법

### 요구사항

- Docker (Docker Compose 포함)
- JDK 17

### 1. 로컬 인프라 기동 (MySQL + LocalStack S3)

```bash
docker compose up -d
docker compose ps 
```

로컬 인프라는 다른 프로젝트와 충돌하지 않도록 기본 포트를 사용하지 않습니다:

| 서비스 | 호스트 포트 | 접속 정보 |
|---|---|---|
| MySQL (`mysql:8.4`) | **3307** | db `dolog` / user `dolog` / pw `localpassword` |
| LocalStack S3 | **4567** | 자격증명 아무 값 (버킷 `dolog-local`은 앱 첫 부팅 시 자동 생성) |

### 2. 앱 실행 (local 프로파일)

```bash
./gradlew bootRun        # bootRun은 프로파일 기본값이 local (ACTIVE=dev 등으로 지정 시 그 값이 우선)
```

IntelliJ에서 메인 클래스를 직접 실행하는 경우에는 실행 구성(Run Configuration)의 환경변수에 `ACTIVE=local`을 지정합니다 (최초 1회).

- API: `http://localhost:8080/api` (동작 확인: `GET /api/exhibitions` → 200)
- Swagger: `http://localhost:8080/api/swagger-ui/index.html`
- 로컬 admin 계정: `admin@local.test` / `admin1234` (부팅 시 자동 생성)

### 3. 테스트 / 빌드

```bash
./gradlew build          # 테스트 포함 — 별도 환경변수가 필요 없습니다 (로컬 도커 컴포즈는 떠 있어야 함)
```

### 로컬 S3(LocalStack) 파일 확인

```bash
aws --endpoint-url=http://localhost:4567 s3 ls s3://dolog-local/ --recursive
```

업로드된 객체는 브라우저에서 직접 열 수 있습니다: `http://localhost:4567/dolog-local/<key>`

### 종료 / 초기화

```bash
docker compose down      # 컨테이너 중지 (데이터 볼륨 유지)
docker compose down -v   # 볼륨까지 삭제 (DB·S3 초기화)
```

## 설정 구조

로컬에 필요한 값은 **`src/main/resources/application-local.properties` 파일 하나**에 있습니다 (전부 로컬 전용 더미 값 — 커밋됨).
별도로 만들거나 채울 파일은 없습니다. 단, 외부 서비스 실제 키가 필요한 기능을 개발할 때만 아래 `config/`를 사용합니다.

| 환경 | 설정 출처 |
|---|---|
| 로컬 | `application-local.properties` (`ACTIVE=local`일 때 로드) |
| 로컬 (실제 키) | `config/application-local.properties` (gitignore, 있으면 위 파일을 덮음) |
| CI | 워크플로(`CI.yml`)가 env 변수 주입 |
| 운영 | 서버 `.env` (GitHub Secret `ENV_FILE`) |

- 실제 운영 시크릿은 로컬 파일·레포에 절대 넣지 않습니다 (GitHub Secret으로만 관리).
- 파일 값과 다르게 쓰고 싶으면 환경변수로 덮을 수 있습니다 (env 변수가 properties 파일보다 우선).
  예: `DATABASE_URL=... ACTIVE=local ./gradlew bootRun`
- 실제 외부 서비스 키(예: OAuth client secret)는 커밋되는 `application-local.properties`에 넣지 않습니다.
  프로젝트 루트에 `config/application-local.properties`를 만들고 거기에만 둡니다. `config/`는 gitignore이고
  Spring Boot가 `file:./config/`를 classpath보다 우선 로드하므로, 실행 시 환경변수 지정이나 추가 설정이 필요 없습니다.
  `src/main/resources` 밖이라 `bootJar` 산출물에도 포함되지 않습니다. 해당 디렉터리를 만들지 않으면 기존과 동일하게 동작합니다.
