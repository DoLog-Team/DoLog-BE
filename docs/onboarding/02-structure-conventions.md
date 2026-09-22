# 2. 프로젝트 구조와 코드 관례

[목차](00-start.md)

## 패키지

`src/main/java/com/dolog/server` 아래를 기준으로 한다.

| 경로 | 역할 |
|---|---|
| domain/account | 계정, 로그인, Refresh Token 세션 |
| domain/exhibition | 전시·참여 작가·구역·지도·주최·파트너·테마 |
| domain/artist | 작가 및 전시별 작가 프로필 |
| domain/artwork | 작품·이미지·작가 연결·순서 |
| domain/bts, domain/banner, domain/plan | 비하인드, 배너, 플랜·구독 |
| global/config | Security, CORS, Swagger, S3, HTTP 설정 |
| global/jwt, global/security | JWT 검증과 인증 사용자 |
| global/response, global/exception | 공통 응답과 예외 처리 |
| global/util, global/order | 파일 처리와 공통 순서 처리 |

도메인은 보통 `web/controller`, `web/dto/request`, `web/dto/response`, `service`, `repository`, `entity`, `exception`으로 나뉜다. Controller → Service → Repository 흐름을 따라 관련 호출부까지 읽고 수정한다. 작품에는 이미지·전시·작가·정렬 서비스와 support 코드가 이미 분리되어 있으므로 기존 경로를 재사용한다.

## 요청과 응답

- 요청 DTO의 Jakarta Validation과 컨트롤러 `@Valid`를 사용한다. 현재 DTO에는 Lombok 클래스와 record가 모두 있다. 주변 코드의 형식을 따른다.
- 성공 응답은 `SuccessResponse.ok/created/of`를 사용한다. 공통 필드는 `isSuccess`, `code`, `message`, `timeStamp`이며 성공/오류 응답에 `httpStatus`, `data`가 추가된다.
- 비즈니스 예외는 기존 도메인 예외와 `BaseResponseCode`를 활용하고 `GlobalExceptionHandler`의 처리 경로를 확인한다. 실제 HTTP 상태와 응답 본문 상태를 함께 검증한다.
- 엔티티를 응답 계약으로 직접 노출하지 않고 기존 응답 DTO로 변환한다. DB 변경은 서비스 트랜잭션 경계와 Flyway를 함께 검토한다.
- Swagger의 `@Operation`, `@Tag`, 인증 API의 `@SecurityRequirement(name = "bearerAuth")` 관례를 따른다.

## 인증·인가

인증 사용자는 `@AuthenticationPrincipal CustomUserDetails`로 받는다. 사용자 ID나 역할을 클라이언트 입력에서 신뢰하지 않는다. 권한은 `DOLOG_ADMIN`, `EXHIBITION_ADMIN`, `ARTIST_ADMIN`이며 `@PreAuthorize`와 서비스의 소유권·상태 검증을 함께 확인한다.

`SecurityConfig`는 전시·작품·작가 관련 일부 GET, 로그인·갱신, Swagger, health를 공개한다. 모든 GET이 공개인 것은 아니다. `/accounts/me` 등은 Bearer Access Token이 필요하다. URL 허용 설정만으로 기능이 구현되었다고 판단하지 않는다. 예를 들어 `/auth/social/login`은 허용 목록에 있지만 현재 AuthController에 구현되어 있지 않다.

JWT는 계정 ID와 세션을 사용한다. Refresh Token은 DB의 로그인 세션과 연결된다. Access/Refresh 용도를 섞지 않는다.

## 파일 및 검증

파일 업로드는 `global/util/FileService`와 도메인 파일 처리 경로를 재사용한다. S3Config가 local과 dev/prod 접속 대상을 분리한다. 기본 multipart 제한은 파일/요청 각각 8MB다.

기존 테스트는 `src/test/java/com/dolog/server` 및 대응 도메인 패키지에 있다. 변경에 맞는 기존 테스트를 실행하고 실행하지 못한 검증을 PR에 남긴다. 새 테스트 파일은 명시적 요청이 있을 때만 생성한다.
