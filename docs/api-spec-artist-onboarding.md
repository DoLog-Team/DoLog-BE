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
- nameKo는 공백 불가/최대 100자, nameEn 최대 100자, phone 최대 30자.
- 전화번호는 기존 관리자 수정과 동일하게 공백/하이픈 제거 및 숫자/중복 검증.

```json
{"nameKo":"김두록"}
```

- 성공: HTTP 200, 응답 data는 `{ "artistId": "UUID" }`.
- 잘못된 입력: 400, 비로그인/유효하지 않은 토큰: 401, 다른 역할: 403.
- Artist 없음 또는 소프트 삭제 상태: 404, 중복 전화번호: 409.
- 전시별 ArtistProfile은 별도이며 이 API에서 갱신하지 않는다.
- 이름이 없어도 기존 전시 참여/작품 흐름에 새 제한을 추가하지 않는다.
- 이름 없는 작가의 알림은 `이름 미등록`, BTS 목록의 작가명은 빈 목록으로 처리한다.

## DB

V18__artist_name_nullable.sql로 artists.name_ko의 NOT NULL 제약만 제거한다.
번호는 사용자 제공 dev DB 이력(V1~V17 성공)과 열린 PR #413/#415의 마이그레이션 목록을 대조하여 정했다.
배포 시 마이그레이션 적용이 필수다. 기존 마이그레이션과 기존 데이터는 수정하지 않는다.
