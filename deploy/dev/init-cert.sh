#!/usr/bin/env bash
# 최초 1회: dev 도메인 인증서 발급 + https 활성화
# 사용: ./init-cert.sh dev.example.com you@email.com
# 전제: DNS A 레코드가 이 서버 IP를 가리키고, 80/443 인그레스가 열려 있을 것
set -euo pipefail
cd "$(dirname "$0")"

DOMAIN="${1:?사용법: ./init-cert.sh <도메인> <이메일>}"
EMAIL="${2:?사용법: ./init-cert.sh <도메인> <이메일>}"

echo "[1/4] nginx 설정에 도메인 반영"
sed -i "s/dev\.example\.com/${DOMAIN}/g" nginx/dev.conf
[ -f nginx/dev-ssl.conf.disabled ] && sed -i "s/dev\.example\.com/${DOMAIN}/g" nginx/dev-ssl.conf.disabled

echo "[2/4] nginx(HTTP) 기동"
docker compose up -d nginx

echo "[3/4] 인증서 발급 (webroot 챌린지)"
docker compose run --rm --entrypoint certbot certbot certonly \
  --webroot -w /var/www/certbot -d "${DOMAIN}" \
  --email "${EMAIL}" --agree-tos --no-eff-email

echo "[4/4] https 설정 활성화 + 리로드"
[ -f nginx/dev-ssl.conf.disabled ] && mv nginx/dev-ssl.conf.disabled nginx/dev-ssl.conf
docker compose exec nginx nginx -s reload

echo "완료 — https://${DOMAIN}/health 로 확인하세요. 갱신은 certbot 컨테이너가 12시간 주기로 자동 수행합니다."
