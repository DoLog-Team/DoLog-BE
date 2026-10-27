# 소셜 가입 시 작가 자동 생성

카카오/구글 소셜 로그인 성공 시 Account에 연결된 Artist가 없으면 같은 트랜잭션에서 생성한다.
소셜 이름은 Artist 이름으로 저장하지 않는다. 최초 nameKo/nameEn/phone은 null이다.
기존 가입자도 다음 소셜 로그인에서 누락된 Artist가 생성된다.
이미 존재하는 Artist의 이름을 덮어쓰지 않으며, 소프트 삭제된 Artist를 자동 복구하지 않는다.
계정 행을 잠가 관리자 등록 및 동시 로그인과 중복 생성을 방지한다.

프론트에서 별도 Artist 생성 API를 호출할 필요가 없다. 로그인 후 기존
`GET /api/accounts/me`로 artistId를 확인한다. 소셜 로그인 응답 형식은 유지한다.

## PATCH /api/artists/me

- 인증: `Authorization: Bearer <accessToken>`, ARTIST_ADMIN 전용
- 요청 계정은 JWT에서 결정한다. accountId/artistId를 받지 않는다.
- nameKo/nameEn/phone 모두 선택. 생략 또는 null은 기존 값을 유지한다.
- nameKo는 빈 문자열이면 국문 성명을 삭제(null 저장)하며, 공백만 있는 문자열은 허용하지 않는다. 최대 100자.
- nameEn 최대 100자, phone 최대 30자.
- 전화번호는 기존 관리자 수정과 동일하게 공백/하이픈 제거 및 숫자/중복 검증.

```json
{"nameKo":"김두록"}
```

- 성공: HTTP 200, 응답 data는 `{ "artistId": "UUID" }`.
- 잘못된 입력: 400, 비로그인/유효하지 않은 토큰: 401, 다른 역할: 403.
- Artist 없음 또는 소프트 삭제 상태: 404, 중복 전화번호: 409.
- 이름을 수정하면 공통 ArtistProfile의 이름도 같은 트랜잭션에서 갱신한다.
- `nameKo: ""`이면 기본정보와 공통 프로필의 국문 성명을 모두 null로 저장한다.
- 이름을 생략하고 전화번호만 수정하면 공통 프로필의 이름은 유지한다.
- 두록 관리자의 작가 기본정보 수정 API에도 같은 이름 갱신 규칙이 적용된다.

## GET /api/artist-profiles/me

- 인증: `Authorization: Bearer <accessToken>`, ARTIST_ADMIN 전용
- 요청 계정은 JWT에서 결정하며 `accountId`, `artistId`, `profileId`를 받지 않는다.
- 전시 참여 승인 후 생성된 작가 공통 프로필을 수정 화면용으로 반환한다.

```json
{
  "artistId": "UUID",
  "profileId": "UUID",
  "nameKo": "김두록",
  "nameEn": "Dolog Kim",
  "bio": "작가 소개",
  "email": "artist@test.com",
  "purchaseContactUrl": "https://open.kakao.com/o/test",
  "profileImg": "https://cdn.test/profile.webp"
}
```

- 성공: HTTP 200.
- 비로그인/유효하지 않은 토큰: 401, 다른 역할: 403.
- Artist 또는 공통 ArtistProfile이 없으면 404.

## PATCH /api/artist-profiles/me

- 인증: `Authorization: Bearer <accessToken>`, ARTIST_ADMIN 전용
- `multipart/form-data`의 `request` JSON part와 선택적 `profileImg` file part를 사용한다.
- `nameKo`, `nameEn`, `bio`, `email`, `purchaseContactUrl`은 모두 선택값이며, 생략 또는 `null`이면 기존 값을 유지한다.
- `nameKo`는 한글·영문·숫자·띄어쓰기만 허용하고 최대 10자, `nameEn`은 영문·띄어쓰기만 허용하고 최대 50자다.
- `nameKo: ""`이면 공통 프로필의 국문 성명을 null로 저장한다. 공백만 있는 문자열은 허용하지 않는다.
- 국문 성명을 삭제한 뒤 `GET /api/artists/{artistId}`에서도 `nameKo: null`을 반환한다.
- `bio`는 최대 200자다.
- 이미지는 JPG/JPEG/PNG, 5MB 이하만 허용한다.

```json
{
  "nameKo": "김두록",
  "nameEn": "Dolog Kim",
  "bio": "작가 소개를 수정했습니다.",
  "email": "artist@test.com",
  "purchaseContactUrl": "https://open.kakao.com/o/test"
}
```

- `profileImg` part 생략: 기존 이미지 유지.
- 빈 `profileImg` part: 기존 이미지 삭제.
- 파일이 있는 `profileImg` part: 새 이미지로 교체.
- 성공: HTTP 200, 응답 data는 `{ "profileId": "UUID" }`.
- 잘못된 입력: 400, 비로그인/유효하지 않은 토큰: 401, 다른 역할: 403.
- Artist 또는 공통 ArtistProfile이 없으면 404.
- `nameKo`, `nameEn`은 공통 프로필의 이름을 갱신한다. 모든 작가 이름 조회·검색은 이 값을 사용한다.
- 프론트에서 이름을 맞추기 위해 `PATCH /api/artists/me`를 추가로 호출할 필요가 없다.
- 전화번호는 이 API에서 변경하지 않는다.

## PATCH /api/artist-profiles/{profileId}

- 인증: `Authorization: Bearer <accessToken>`.
- `ARTIST_ADMIN`은 자신의 공통 프로필만 수정할 수 있다.
- `DOLOG_ADMIN`은 모든 작가의 공통 프로필을 수정할 수 있다.
- `EXHIBITION_ADMIN`은 작가 전체에 적용되는 공통 프로필을 수정할 수 없다.
- 요청 형식, 수정 필드, 이미지 처리 및 응답은
  `PATCH /api/artist-profiles/me`와 동일하다.
- 다른 작가의 프로필을 수정하면 403, 프로필이 존재하지 않으면 404.

## DB

작가 이름의 조회·검색 기준은 `artist_profiles.name_ko/name_en`이다.
기존 `artists.name_ko/name_en`은 최초 프로필 생성용 기본값과 기존 기본정보 수정 API의 호환을 위해 남긴다.
두 값이 다르면 공통 프로필 값을 사용하며, 공통 프로필 이름이 `null`인 경우에도 기본정보 이름으로 대체하지 않는다.
이 이름 기준 변경에는 추가 스키마 마이그레이션이 필요하지 않다.

V18__artist_name_nullable.sql로 artists.name_ko의 NOT NULL 제약을 제거한다.
V19__remove_artist_profile_visibility.sql로 프로필 공개 여부 컬럼을 제거한다.
V20__make_artist_profile_global.sql로 작가별 대표 프로필 하나를 남기고 전시 외래 키를 제거한다.
기존 프로필의 조회수는 합산하고 좋아요·SNS·BTS·작품 연결은 대표 프로필로 이관한다.
배포 시 마이그레이션 적용이 필수다. 기존 마이그레이션과 기존 데이터는 수정하지 않는다.
