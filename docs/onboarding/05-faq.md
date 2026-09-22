# 5. FAQ

[목차](00-start.md)

| 증상 | 확인할 것 |
|---|---|
| IDE 실행에서 환경변수 누락 | 실행 구성에 ACTIVE=local 지정. bootRun만 local 기본값을 주입한다. |
| DB connection refused | docker compose ps, MySQL healthy, localhost:3307, DATABASE_URL 덮어쓰기 여부 |
| 8080/9090/3307/4567 포트 충돌 | 다른 앱·컨테이너 확인. 포트 변경 시 Compose와 앱 설정을 함께 맞춘다. |
| Flyway checksum mismatch | 적용된 SQL 수정 여부 확인. 새 마이그레이션으로 변경한다. |
| Hibernate validate 실패 | 적용 이력과 엔티티 차이 확인. ddl-auto=update로 우회하지 않는다. |
| 시드 수정이 안 보임 | INSERT IGNORE는 기존 행을 갱신하지 않는다. 로컬 데이터 삭제 가능 여부를 먼저 판단한다. |
| 401 | Bearer 형식, Access Token 만료, 세션과 계정 상태 확인 |
| 시드 전시 로그인 실패 | LOCAL001은 허용 코드 형식에 맞지 않는다. 이메일 로그인도 DOLOG_ADMIN 전용이다. 유효한 전시 테스트 데이터가 필요하다. |
| 403 | 역할과 리소스 소유권 확인. 관리자 토큰과 작가 토큰을 구분한다. |
| 개발용 작가 로그인 실패 | local/dev 프로파일과 DOLOG_ADMIN 토큰 필요. prod에서는 컨트롤러가 없다. |
| 관리자 암호 설정을 바꿔도 로그인 실패 | AdminInitializer는 기존 이메일이 있으면 재생성하지 않는다. 기존 계정 암호 변경 흐름을 사용한다. |
| Postman은 성공, 브라우저만 실패 | WebConfig의 CORS 허용 origin과 요청 origin 대조 |
| S3 업로드 실패 | LocalStack 상태·버킷 생성 경고·8MB 제한 확인 |
| health 404 | 로컬 주소는 9090/actuator/health, /api 접두사 없음 |
| 테스트 기동 실패 | JDK 17 및 로컬 DB 확인. 테스트는 DB 없는 단위 테스트만으로 구성되어 있지 않다. |

현재 CORS 허용 origin은 `http://localhost:5173`, `https://dolog.kr`, `https://www.dolog.kr`다. 실제 오류는 로그와 HTTP 상태를 함께 확인하고, 토큰·비밀번호를 로그나 PR에 붙이지 않는다.
