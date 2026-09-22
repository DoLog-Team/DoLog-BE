# 7. Postman으로 API 확인

[목차](00-start.md)

## 환경

Postman Environment에 `baseUrl=http://localhost:8080/api`, `accessToken`, `refreshToken`을 만든다. 토큰은 로컬 값으로 보관하고 공유/export하지 않는다. Swagger의 OpenAPI JSON(`/api/v3/api-docs`)을 Import해 요청을 만들 수도 있다.

이 프로젝트는 Bearer JWT를 사용한다. 다른 프로젝트의 `X-Dev-User` 헤더는 지원하지 않는다.

## 관리자 로그인

`POST {{baseUrl}}/auth/login`, Authorization은 No Auth, Body는 raw JSON:

```json
{"email":"admin@local.test","password":"admin1234"}
```

성공 응답의 `data.accessToken`, `data.refreshToken`을 환경변수에 저장한다. Post-response script를 사용한다면:

```javascript
pm.test("로그인 성공", () => pm.response.to.have.status(200));
if (pm.response.code === 200) {
  const data = pm.response.json().data;
  pm.environment.set("accessToken", data.accessToken);
  pm.environment.set("refreshToken", data.refreshToken);
}
```

보호 API는 Authorization → Bearer Token → `{{accessToken}}`을 설정한다. `GET {{baseUrl}}/accounts/me`로 현재 역할을 확인한다.

## 다른 역할

| 요청 | Body | 인증 |
|---|---|---|
| POST /auth/exhibition/login | `{"entryCode":"{{entryCode}}"}` | No Auth |
| POST /auth/dev/artist-login | `{"fixture":"ARTIST_1"}` | DOLOG_ADMIN Bearer |

전시 로그인은 환경변수 `entryCode`에 실제 전시의 유효한 8자리 코드를 넣는다. 허용 문자는 숫자 2~9와 I/L/O를 제외한 영문자다. 현재 시드 `LOCAL001`은 이 형식에 맞지 않아 거절된다. 전시 시드 계정의 이메일/비밀번호도 관리자 전용 `/auth/login`으로 로그인할 수 없다. 코드의 만료 및 계정·전시 이용 상태 검증도 통과해야 한다.

작가 로그인은 local/dev에서만 가능하고 prod에서는 없다. 반환받은 작가 토큰을 사용해야 작가 역할 테스트가 된다. 관리자 토큰은 별도 변수로 보관하면 역할 전환이 쉽다. ARTIST_1은 로컬 시드 계정을 재사용하며 ARTIST_2도 개발용 fixture로 지원한다.

## 갱신·로그아웃·실패 확인

`POST {{baseUrl}}/auth/refresh`에 No Auth로 다음 JSON을 보낸다. 반환된 토큰을 다시 저장한다.

```json
{"refreshToken":"{{refreshToken}}"}
```

`POST {{baseUrl}}/auth/logout`은 현재 Access Token으로 호출하며 현재 로그인 세션을 로그아웃한다.

1. No Auth로 `GET /exhibitions` 공개 조회를 확인한다.
2. 정상 토큰으로 `GET /accounts/me`를 확인한다.
3. No Auth로 같은 보호 API를 호출해 401을 확인한다.
4. 작가 토큰으로 개발용 작가 로그인(관리자 전용)을 호출해 403을 확인한다.
5. 로그아웃 후 해당 세션의 토큰 사용이 거절되는지 확인한다.

`/auth/social/login`은 현재 컨트롤러에 구현되지 않았다. 개발용 작가 로그인은 실제 소셜 연동 검증이 아니다.
