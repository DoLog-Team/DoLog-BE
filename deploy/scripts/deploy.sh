#!/usr/bin/env bash
# 서버에서 실행하는 배포 스크립트 — deploy-dev.yml / deploy-prod.yml이 scp로 올린 뒤 호출한다.
# 사용: deploy.sh <dev|prod> <image-ref>
#   image-ref: <user>/be:<커밋 SHA> 또는 <user>/be@sha256:...
# 환경변수: ENV_FILE(필수, .env 전체), DOCKER_USERNAME·DOCKER_PASSWORD(선택)
# 수동 실행(서버에서, 현재 .env 재사용): ENV_FILE="$(cat .env)" bash deploy.sh prod <image-ref>
set -euo pipefail

ENV_NAME=${1:?사용: deploy.sh <dev|prod> <image-ref>}
REF=${2:?사용: deploy.sh <dev|prod> <image-ref>}
: "${ENV_FILE:?ENV_FILE 환경변수가 필요합니다}"

case "$ENV_NAME" in
  dev)  IMAGE_KEY=DEV_IMAGE;  CONTAINER=be-dev ;;
  prod) IMAGE_KEY=PROD_IMAGE; CONTAINER=be ;;
  *) echo "알 수 없는 환경: $ENV_NAME (dev|prod)"; exit 2 ;;
esac
SSL_CONF="nginx/${ENV_NAME}-ssl.conf"

cd "$(dirname "$0")"
umask 077

health() {
  for _ in $(seq 1 30); do
    curl -fsS http://localhost:9090/actuator/health > /dev/null 2>&1 && return 0
    sleep 5
  done
  return 1
}

# .env = ENV_FILE + 이미지 줄. ENV_FILE에 이미지 줄이 있어도 무시한다(이 스크립트가 정본)
write_env() {
  printf '%s\n' "$ENV_FILE" | grep -v "^${IMAGE_KEY}=" > .env || true
  echo "${IMAGE_KEY}=$1" >> .env
}

if [ -n "${DOCKER_PASSWORD:-}" ]; then
  echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
fi

echo "[1/4] Pull $REF"
docker pull "$REF"   # 없는 이미지면 여기서 실패 — 서버 상태는 아직 그대로

# 롤백 대상: 지금 떠 있는 컨테이너의 이미지를 digest ref로 (태그는 그새 바뀌었을 수 있음)
PREV_REF=""
if PREV_ID=$(docker inspect --format '{{.Image}}' "$CONTAINER" 2>/dev/null); then
  PREV_REF=$(docker image inspect --format '{{index .RepoDigests 0}}' "$PREV_ID" 2>/dev/null || true)
fi
if [ -f .env ]; then cp -p .env .env.prev; fi

if [ "$ENV_NAME" = prod ]; then
  # compose recreate가 json-file 로그를 지우므로 직전 로그 보존
  mkdir -p "$HOME/logs"
  docker logs "$CONTAINER" > "$HOME/logs/$CONTAINER-$(date +%Y%m%d-%H%M%S).log" 2>&1 || true
fi

write_env "$REF"

# 인증서 발급 이후에는 레포의 ssl 설정(.disabled)이 활성본을 대체 — 레포가 항상 정본
if [ -f "$SSL_CONF" ] && [ -f "$SSL_CONF.disabled" ]; then
  mv -f "$SSL_CONF.disabled" "$SSL_CONF"
fi

echo "[2/4] Up app"
# MySQL은 꺼져 있을 때만 띄운다 — compose 설정 변경으로 DB가 재생성되지 않게. DB 설정 변경은 수동 적용
if [ -z "$(docker compose ps -q --status running mysql)" ]; then
  docker compose up -d --wait mysql
fi
docker compose up -d --no-deps app
# nginx/certbot은 최초 인증서 발급(init-cert.sh) 이후에만 기동 가능
if [ -f "$SSL_CONF" ]; then
  docker compose up -d --no-deps nginx certbot
  docker compose exec -T nginx nginx -s reload || true
else
  echo "(안내) 아직 인증서 미발급 상태 — ./init-cert.sh <도메인> <이메일> 실행 필요"
fi

echo "[3/4] Health check (최대 150초)"
if health; then
  echo "Health check OK"
  echo "[4/4] Cleanup"
  docker image prune -f
  exit 0
fi

echo "Health check 실패"
docker logs --tail 100 "$CONTAINER" || true
if [ -z "$PREV_REF" ] || [ ! -f .env.prev ]; then
  echo "롤백할 이전 이미지/.env가 없습니다"
  exit 1
fi

echo "롤백: $PREV_REF (.env도 이전 값으로 복구)"
cp -p .env.prev .env
sed -i "/^${IMAGE_KEY}=/d" .env
echo "${IMAGE_KEY}=${PREV_REF}" >> .env
docker compose up -d --no-deps app
if health; then
  echo "롤백 후 Health check OK"
else
  echo "롤백 후에도 Health check 실패 — 수동 확인 필요"
  docker logs --tail 100 "$CONTAINER" || true
fi
exit 1
