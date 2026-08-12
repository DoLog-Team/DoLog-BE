#!/usr/bin/env bash
# prod 백업(S3) → dev MySQL 동기화
# 사용: Oracle dev 서버의 deploy/dev/ 에서  ./sync-dev-db.sh
# 필요: .env (BACKUP_BUCKET, BACKUP_AWS_ACCESS_KEY_ID/SECRET reader, AWS_REGION, DEV_MYSQL_ROOT_PASSWORD)
#       aws CLI, dev-mysql 컨테이너 기동 상태
set -euo pipefail
cd "$(dirname "$0")"
set -a; source .env; set +a

export AWS_ACCESS_KEY_ID="$BACKUP_AWS_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$BACKUP_AWS_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION="${AWS_REGION:-ap-northeast-2}"

echo "[1/3] 최신 prod 백업 받기 (S3)"
LATEST=$(aws s3api list-objects-v2 --bucket "$BACKUP_BUCKET" --prefix db/ \
  --query 'sort_by(Contents,&LastModified)[-1].Key' --output text)
[ "$LATEST" = "None" ] && { echo "ERROR: 백업 없음"; exit 1; }
DUMP="/tmp/$(basename "$LATEST")"
aws s3 cp "s3://$BACKUP_BUCKET/$LATEST" "$DUMP" --only-show-errors
trap 'rm -f "$DUMP"' EXIT
echo "  $LATEST ($(du -h "$DUMP" | cut -f1))"

echo "[2/3] dev MySQL 적재 (기존 데이터 대체)"
docker exec -i dev-mysql mysql -u root -p"${DEV_MYSQL_ROOT_PASSWORD}" \
  -e "DROP DATABASE IF EXISTS dolog; CREATE DATABASE dolog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
gunzip -c "$DUMP" | docker exec -i dev-mysql mysql -u root -p"${DEV_MYSQL_ROOT_PASSWORD}" dolog

echo "[3/3] 확인"
docker exec dev-mysql mysql -u root -p"${DEV_MYSQL_ROOT_PASSWORD}" -N \
  -e "SELECT COUNT(*) AS tables_cnt FROM information_schema.tables WHERE table_schema='dolog';"

echo "완료 — dump 파일은 자동 정리됩니다"
