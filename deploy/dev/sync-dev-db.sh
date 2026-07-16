#!/usr/bin/env bash
# prod RDS → dev MySQL 컨테이너 동기화 (온디맨드)
# 사용: Oracle dev 서버의 deploy/dev/ 에서  ./sync-dev-db.sh
# 필요: .env (PROD_RDS_HOST, PROD_DB_USER, PROD_DB_PASSWORD, TUNNEL_HOST, TUNNEL_SSH_KEY, DEV_MYSQL_ROOT_PASSWORD)
#       mysql-client(mysqldump), dev-mysql 컨테이너 기동 상태
set -euo pipefail
cd "$(dirname "$0")"

set -a; source .env; set +a

TUNNEL_PORT=13306
DUMP_FILE="dolog-$(date +%Y%m%d-%H%M%S).sql"

echo "[1/4] SSH 터널 열기 (localhost:${TUNNEL_PORT} → RDS)"
ssh -f -N -o ExitOnForwardFailure=yes -i "${TUNNEL_SSH_KEY}" \
  -L "${TUNNEL_PORT}:${PROD_RDS_HOST}:3306" "ubuntu@${TUNNEL_HOST}"
TUNNEL_PID=$(pgrep -f "ssh.*${TUNNEL_PORT}:${PROD_RDS_HOST}" | head -1)
trap 'kill "${TUNNEL_PID}" 2>/dev/null || true; rm -f "${DUMP_FILE}"' EXIT

echo "[2/4] mysqldump (--single-transaction: 락 없는 일관 스냅샷, prod 무영향)"
mysqldump -h 127.0.0.1 -P "${TUNNEL_PORT}" -u "${PROD_DB_USER}" -p"${PROD_DB_PASSWORD}" \
  --single-transaction --routines --triggers --set-gtid-purged=OFF \
  dolog > "${DUMP_FILE}"
echo "  dump 크기: $(du -h "${DUMP_FILE}" | cut -f1)"

echo "[3/4] dev MySQL에 적재 (기존 데이터 대체)"
docker exec -i dev-mysql mysql -u root -p"${DEV_MYSQL_ROOT_PASSWORD}" \
  -e "DROP DATABASE IF EXISTS dolog; CREATE DATABASE dolog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
docker exec -i dev-mysql mysql -u root -p"${DEV_MYSQL_ROOT_PASSWORD}" dolog < "${DUMP_FILE}"

echo "[4/4] 확인"
docker exec dev-mysql mysql -u root -p"${DEV_MYSQL_ROOT_PASSWORD}" -e "SELECT COUNT(*) AS tables_cnt FROM information_schema.tables WHERE table_schema='dolog';"

echo "완료 — 터널과 dump 파일은 자동 정리됩니다"
