# 8. 배포와 운영

[목차](00-start.md)

## 환경과 근거

배포 파일은 `deploy/dev`, `deploy/prod`, 자동화는 `.github/workflows`에 있다. 아래 서버 배치와 AWS 보존 정책은 기존 Notion 운영 기록이며 이번 작업에서 실환경을 조회하지 않았다. 실행 전에 현재 콘솔·서버 설정을 확인한다.

| 환경 | 기존 기록의 위치 | 컨테이너 | 도메인 |
|---|---|---|---|
| dev | Oracle A1, ~/dolog-dev | be-dev, dev-mysql, dev-nginx, dev-certbot | dev.dolog-archive.site |
| prod | Oracle A1, ~/dolog-prod | be, prod-mysql, prod-nginx, prod-certbot | dolog-archive.site |

Oracle는 Compute와 VCN을 사용하고 이미지 및 DB 백업은 AWS S3를 사용한다. 인스턴스 사양·요금·무료 한도는 변경될 수 있으므로 이 문서에서 보장하지 않는다.

## 배포와 롤백

- dev: dev 브랜치 CI 성공 후 자동 배포된다. 수동 workflow_dispatch 경로도 있다.
- prod: CI를 통과한 dev 커밋에서 새 `vX.Y.Z` 정식 Release를 발행한다. pre-release는 제외된다. 운영 워크플로는 테스트를 재실행하지 않으므로 발행 전에 대상 커밋의 CI를 확인한다.
- 호환성 깨짐은 major, 호환 기능 추가는 minor, 수정은 patch를 올린다. 기존 태그를 재사용하지 않는다.

GitHub Releases에서 새 태그와 대상 커밋을 선택하고 변경 내용을 작성한 후 Publish release하면 운영 배포가 시작된다. `.github/workflows/deploy-prod.yml`은 이미지 빌드·게시, 서버 파일 복사, `.env`와 PROD_IMAGE 기록, 컨테이너 재기동, 관리 포트 9090 health 확인을 수행한다. 실패 시 이전 이미지로 돌아가는 경로가 있다. **이미지 롤백은 DB 마이그레이션을 되돌리지 않는다.**

수동 롤백은 DB가 이전 앱과 호환되는지 확인한 뒤 서버 `.env`의 `PROD_IMAGE`를 이전 정상 태그로 갱신하고 `docker compose up -d app`을 실행한다. 실행 환경변수만 일시 변경하면 다음 기동 때 다른 이미지가 선택될 수 있다.

## 설정과 접근 권한

GitHub Environments `development`/`production`의 SSH_HOST, SSH_USER, SSH_KEY, ENV_FILE 등은 워크플로에서 참조한다. 실제 키는 문서에 기록하지 않는다. 서버 `.env`는 배포 시 덮어쓰므로 영구 변경은 해당 GitHub Secret에 반영한다. 필요한 키 이름은 `deploy/dev/.env.example`, `deploy/prod/.env.example`을 참고한다.

앱용 S3 키, 백업 writer, 백업 reader를 구분한다. 원본 Notion에 기록된 AWS 키 원문과 SSH 키 첨부는 공유 문서로 옮기지 않는다. 팀의 안전한 자격증명 전달 경로를 이용한다.

SSH 키는 저장소 밖에 두고 권한을 600으로 설정한다. `~/.ssh/config`에 아래처럼 환경별 별칭을 구성하면 문서의 명령을 사용할 수 있다. 호스트와 키 경로는 담당자에게 확인한다.

```text
Host dolog-prod
    HostName <운영 호스트>
    User ubuntu
    IdentityFile ~/.ssh/dolog/dolog-prod.pem
    IdentitiesOnly yes

Host dolog-dev
    HostName <개발 호스트>
    User ubuntu
    IdentityFile ~/.ssh/dolog/dolog-dev.pem
    IdentitiesOnly yes
```

## 백업·복구·dev 동기화

`deploy/prod/backup-db.sh`는 MySQL을 `--single-transaction`으로 덤프하고 gzip 검증 및 최소 크기를 확인한다. 최근 24시간 앱 로그와 함께 S3의 `db/`, `log/`에 올리고 일요일에는 `weekly/`에도 복사한다. 로컬 3일 초과 파일은 정리한다. AWS CLI나 writer 키가 없으면 원격 업로드를 건너뛸 수 있으므로 마지막 OK 출력만 보지 말고 실제 S3 객체를 확인한다.

서버 cron은 배포만으로 등록·변경되지 않는다. `timedatectl`과 `crontab -l`을 먼저 확인한다. 스크립트의 권장 시각은 UTC 19:00(KST 다음 날 04:00)이다. `backup-check.yml`은 UTC 21:00에 최신 백업의 26시간 이내 여부와 10KB 이상 크기를 검사하고 실패 시 Discord로 알린다.

기존 기록의 S3 수명주기는 `db/`, `log/` 14일, `weekly/` 56일이다. 실제 버킷 정책은 별도 확인한다.

복구는 먼저 격리 MySQL에서 리허설한다. reader 권한으로 선택한 덤프를 내려받고 `gzip -t`로 검사한 뒤 대상 DB에 복원한다. 테이블 수를 고정값으로 판단하지 말고 Flyway 이력·주요 데이터·앱 기동을 확인한다. 운영 복원은 대상 데이터 보존과 작업 시간을 정한 후 수행한다.

`deploy/dev/sync-dev-db.sh`는 최신 S3 백업을 내려받아 **dev의 dolog DB를 DROP/CREATE한 뒤 복원**한다. prod에는 직접 접속하지 않지만 dev 데이터는 없어지고 운영 데이터가 dev에 복제된다. dev 데이터 보존·접근 권한·현재 앱과 백업 스키마의 호환성을 확인하고 앱 쓰기를 중지한 후 실행한다.

```bash
# dev 서버에서, 기존 dev 데이터를 대체하기로 결정한 경우에만
cd ~/dolog-dev
docker compose stop app
./sync-dev-db.sh
docker compose up -d app
```

실패하면 앱을 재기동하기 전에 복구 결과를 확인한다. 스크립트 자체에는 앱 중지나 마이그레이션 호환성 확인이 포함되어 있지 않다.

## 네트워크·TLS·장애 확인

DNS, OCI Security List와 호스트 방화벽을 각각 확인한다. 외부 서비스 포트는 HTTP/HTTPS이며 DB·앱 관리 포트를 임의로 공개하지 않는다. 기존 기록상 DB는 서버 루프백 3306으로 접근하며 GUI에서는 SSH 터널을 사용한다.

nginx 설정은 `deploy/*/nginx`가 정본이다. 인증서는 각 환경의 `init-cert.sh`로 최초 발급하고 certbot으로 갱신한다. 도메인이나 인증서 경로를 바꾸면 레포의 설정과 일치시킨다. 서버 파일만 수정하면 다음 배포에서 덮인다.

```bash
ssh dolog-prod 'docker ps --format "{{.Names}}  {{.Status}}"'
ssh dolog-prod 'docker logs be --tail 100'
ssh dolog-prod 'docker logs prod-nginx --tail 100'
curl -fsS https://dolog-archive.site/health
```

HTTPS용 nginx 설정에서 HEAD `/health`는 nginx가 바로 200을 반환하고 GET은 앱 health를 조회한다. 기존 Notion의 UptimeRobot 감시는 HEAD 기반으로 기록되어 있으므로 엣지 정상과 앱 정상을 구분한다. 실제 활성 nginx 설정과 모니터 설정은 서버에서 확인한다.

Oracle 재구축 시 컴파트먼트(dev/prod), ARM 이미지, VCN/서브넷, SSH 공개키, 호스트 방화벽을 확인한다. IP 변경은 DNS와 GitHub SSH_HOST에도 반영하고 TLS·health·백업 cron을 점검한다. 오래된 문서의 방화벽 규칙 번호나 과금 한도를 그대로 적용하지 않는다.
