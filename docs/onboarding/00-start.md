# DoLog 백엔드 온보딩

전시·작가·작품을 관리하는 전시 아카이브 백엔드다. 이미지 파일은 S3에 저장하고 JWT로 인증한다.

## 읽는 순서

| 문서 | 필요한 시점 |
|---|---|
| [01. 로컬 실행](01-local-run.md) | 처음 실행할 때 |
| [02. 구조와 코드 관례](02-structure-conventions.md) | API를 수정하기 전 |
| [03. Flyway](03-flyway.md) | 스키마를 바꿀 때 |
| [04. Git·PR·CI](04-git-pr-ci.md) | 작업 시작과 PR 제출 |
| [05. FAQ](05-faq.md) | 실행·인증 오류 해결 |
| [06. DB 조회](06-db-queries.md) | 로컬 데이터 확인 |
| [07. Postman](07-postman.md) | 역할별 API 확인 |
| [08. 배포와 운영](08-operations.md) | 배포·백업·복구·서버 관리 |

## 기술 스택과 환경

현재 체크아웃의 `build.gradle` 기준: Java 17, Spring Boot 3.5.16, Gradle 8.14.5, Spring MVC/Data JPA/Validation/Security/Actuator, Flyway, JJWT 0.13.0, springdoc 2.9.0, AWS SDK S3 2.54.1, Scrimage 4.6.7이다.

| 환경 | 앱 | DB | 파일 저장 | 설정 |
|---|---|---|---|---|
| local | IDE 또는 Gradle | MySQL 8.4, localhost:3307 | LocalStack, localhost:4567 | application-local.properties |
| CI | Gradle 테스트 | MySQL 8.0 서비스 | 테스트 구성 및 mock | CI.yml 환경변수 |
| dev/prod | Docker Compose | MySQL 8.4 | AWS S3 | 서버 .env 및 GitHub Secrets |

모든 환경에서 Flyway가 스키마를 적용하고 Hibernate는 `validate`만 수행한다. 로컬에서만 `db/seed`의 시드를 추가 적용한다.

## 문서의 기준

[기존 Notion SETUP](https://app.notion.com/p/3bd31636d6cb8177bd2fe3262bf3eec0)을 분류하고 현재 저장소 코드와 대조했다. 코드 기준점은 `4c03e92`(2026-09-22 확인)이며, 이후 변경은 관련 문서도 함께 수정한다. Notion의 이전 Boot 버전, 로컬 `ddl-auto=update`, V1만 존재한다는 설명은 현재 코드와 달라 정정했다.

운영 서버 상태, 실제 마이그레이션 이력, 클라우드 권한·요금·보존 정책은 이번 문서 작업에서 실환경 확인하지 않았다. [운영 문서](08-operations.md)의 기존 기록과 코드 확인 사항을 구분해서 읽는다. 원본의 비밀키·첨부 키 파일·임시 서명 이미지 URL은 복사하지 않는다.

QR은 더미 작품을 먼저 만들고 `artworks.id`로 프론트에서 발급한다. 별도 QR 연결 테이블이나 `/q` 리다이렉트를 추가하는 설계는 사용하지 않는다.
