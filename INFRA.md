# 인프라 및 Swagger 정보

## Swagger UI

| 환경 | URL |
| --- | --- |
| 로컬 | http://localhost:8080/api/swagger-ui/index.html |
| 배포 | https://api.dolog-archive.site/api/swagger-ui/index.html |

### 인증 방법

1. Swagger 접속
2. 우측 상단 `Authorize` 버튼 클릭
3. `POST /auth/login` 으로 토큰 발급
4. `Bearer {토큰}` 입력 후 인증

---

## 배포 서버

- **API 주소**: https://api.dolog-archive.site/api
- **서버**: AWS EC2 (`ubuntu@3.37.92.241`, Port 22)

---

## CI/CD

GitHub Actions 워크플로 2개로 구성돼 있어.

### 흐름

```
PR 또는 push (dev/main)
    ↓
[CI] 빌드 + 테스트 + Docker 이미지 빌드 & Docker Hub 푸시
    ↓ CI 성공 시 자동 트리거
[CD] EC2에 SSH 접속 → 새 이미지 pull → 컨테이너 재시작
```

### CI (`.github/workflows/CI.yml`)

트리거: `dev`, `main` 브랜치에 PR 또는 push

1. MySQL 컨테이너 띄우기 (테스트용)
2. Gradle 빌드 + 테스트 실행
3. PR이 아닌 경우(dev/main push)에만 Docker 이미지 빌드 후 Docker Hub 푸시
   - 이미지 태그: `latest` + `{커밋 SHA}`

### CD (`.github/workflows/CD.yml`)

트리거: CI 워크플로가 성공으로 완료됐을 때 자동 실행

1. EC2에 SSH 접속
2. GitHub Secrets의 `ENV_FILE` 값을 서버에 `.env` 파일로 저장
3. Docker Hub에서 최신 이미지 pull
4. 기존 컨테이너 중지 및 삭제 후 새 컨테이너 실행 (포트 8080)
5. 사용하지 않는 이미지 정리

### GitHub Secrets 목록

| Secret 이름 | 용도 |
| --- | --- |
| `DOCKER_USERNAME` | Docker Hub 계정 |
| `DOCKER_PASSWORD` | Docker Hub 비밀번호 |
| `SSH_HOST` | EC2 IP (`3.37.92.241`) |
| `SSH_USER` | EC2 유저명 (`ubuntu`) |
| `SSH_KEY` | EC2 SSH 프라이빗 키 |
| `ENV_FILE` | 서버에서 사용하는 환경변수 전체 (DB, JWT, S3 등) |
| `ACCESS_KEY` | NCP Object Storage Access Key |
| `SECRET_KEY` | NCP Object Storage Secret Key |
| `BUCKET_NAME` | NCP Bucket 이름 |
| `JWT_SECRET_KEY` | JWT 서명 키 |

## 데이터베이스

- **종류**: AWS RDS MySQL 8.x
- **Host**: dolog-db.cvicw6i064my.ap-northeast-2.rds.amazonaws.com
- **Port**: 3306
- **Schema**: dolog

## 파일 스토리지

- **NCP Object Storage** (AWS S3 호환)
- **Bucket**: dolog-bucket
