# 소셜 가입 시 작가 자동 생성

카카오/구글 소셜 로그인 성공 시 Account에 연결된 Artist가 없으면 같은 트랜잭션에서 생성한다.
소셜 이름은 Artist 이름으로 저장하지 않는다. 최초 nameKo/nameEn/phone은 null이다.
기존 가입자도 다음 소셜 로그인에서 누락된 Artist가 생성된다.
이미 존재하는 Artist의 이름을 덮어쓰지 않으며, 소프트 삭제된 Artist를 자동 복구하지 않는다.
계정 행을 잠가 관리자 등록 및 동시 로그인과 중복 생성을 방지한다.

프론트에서 별도 Artist 생성 API를 호출할 필요가 없다. 로그인 후 기존
`GET /api/accounts/me`로 artistId를 확인한다. 소셜 로그인 응답 형식은 유지한다.

## GET /api/artists/me

- 인증: `Authorization: Bearer <accessToken>`, ARTIST_ADMIN 전용
- 요청 계정은 JWT에서 결정하며 `accountId`, `artistId`, `profileId`를 받지 않는다.
- 전시 참여 승인 후 생성된 작가 공통 프로필을 수정 화면용으로 반환한다.

```json
{
  "artistId": "UUID",
  "profileId": "UUID",
  "nameKo": "김두록",
  "nameEn": "Dolog Kim",
  "phone": "01012345678",
  "bio": "작가 소개",
  "email": "artist@test.com",
  "purchaseContactUrl": "https://open.kakao.com/o/test",
  "profileImg": "https://cdn.test/profile.webp"
}
```

- 성공: HTTP 200.
- 비로그인/유효하지 않은 토큰: 401, 다른 역할: 403.
- Artist 또는 공통 ArtistProfile이 없으면 404.

## PATCH /api/artists/me

- 인증: `Authorization: Bearer <accessToken>`, ARTIST_ADMIN 전용
- `multipart/form-data`의 `request` JSON part와 선택적 `profileImg` file part를 사용한다.
- `nameKo`, `nameEn`, `phone`, `bio`, `email`, `purchaseContactUrl`은 모두 선택값이며, 생략 또는 `null`이면 기존 값을 유지한다.
- `nameKo`는 한글·영문·숫자·띄어쓰기만 허용하고 최대 10자, `nameEn`은 영문·띄어쓰기만 허용하고 최대 50자다.
- `bio`는 최대 200자, `phone`은 최대 30자다.
- 전화번호는 기존 관리자 수정과 동일하게 공백·하이픈 제거 및 숫자·중복 검증을 수행한다.
- 이미지는 JPG/JPEG/PNG, 5MB 이하만 허용한다.

```json
{
  "nameKo": "김두록",
  "nameEn": "Dolog Kim",
  "phone": "010-9999-8888",
  "bio": "작가 소개를 수정했습니다.",
  "email": "artist@test.com",
  "purchaseContactUrl": "https://open.kakao.com/o/test"
}
```

- `profileImg` part 생략: 기존 이미지 유지.
- 빈 `profileImg` part: 기존 이미지 삭제.
- 파일이 있는 `profileImg` part: 새 이미지로 교체.
- 성공: HTTP 200, 응답 data는 `{ "artistId": "UUID" }`.
- 잘못된 입력: 400, 비로그인/유효하지 않은 토큰: 401, 다른 역할: 403.
- Artist 또는 공통 ArtistProfile이 없으면 404, 중복 전화번호: 409.
- 작가의 기본 이름과 공통 프로필 이름을 함께 갱신한다.

## DB

V18__artist_name_nullable.sql로 artists.name_ko의 NOT NULL 제약만 제거한다.
번호는 사용자 제공 dev DB 이력(V1~V17 성공)과 열린 PR #413/#415의 마이그레이션 목록을 대조하여 정했다.
배포 시 마이그레이션 적용이 필수다. 기존 마이그레이션과 기존 데이터는 수정하지 않는다.
