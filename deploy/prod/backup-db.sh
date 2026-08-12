#!/usr/bin/env bash
# prod MySQL 컨테이너 → gzip 덤프 → (원격 오브젝트 스토리지 업로드) + 앱 로그 동봉(L1 내구성)
# 실행은 서버 crontab, 정의는 레포(정본)
# 사용: prod 서버의 deploy/prod/ 에서  ./backup-db.sh   (크론 등록은 아래 참고)
# 필요: prod-mysql 컨테이너 기동, aws CLI + .env의 BACKUP_AWS_ACCESS_KEY_ID/SECRET/BACKUP_BUCKET/AWS_REGION
#
# 크론 등록 (서버, 1회만 실행해주면 됨!!):
#   crontab -e
#   0 4 * * * /home/ubuntu/dolog-prod/backup-db.sh >> /home/ubuntu/logs/backup.log 2>&1
#
# 원격 보존 = 버킷 수명주기 규칙이 담당 (db/ 14일, log/ 14일). 이 스크립트는 로컬 3일만 정리.
set -euo pipefail
cd "$(dirname "$0")"
set -a; source .env; set +a

TS=$(date +%Y%m%d-%H%M%S)
BK_DIR="${BK_DIR:-$HOME/backups}"; mkdir -p "$BK_DIR"
DUMP="$BK_DIR/dolog-$TS.sql.gz"

echo "[1/4] mysqldump (--single-transaction: 락 없는 일관 스냅샷) → gzip"
# 비번은 컨테이너 내부 $MYSQL_ROOT_PASSWORD로 확장
docker exec prod-mysql sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers --set-gtid-purged=OFF dolog' \
  | gzip > "$DUMP"

echo "[2/4] 덤프 무결성 검증 (빈 파일/손상 업로드 방지)"
if ! gzip -t "$DUMP" 2>/dev/null || [ "$(stat -c%s "$DUMP")" -lt 1000 ]; then
  echo "ERROR: 덤프 손상/과소($(stat -c%s "$DUMP" 2>/dev/null || echo 0)B) — 중단"; rm -f "$DUMP"; exit 1
fi
echo "  덤프 크기: $(du -h "$DUMP" | cut -f1)"

echo "[3/4] 앱 로그 동봉 (L1 — 인스턴스 소실 대비)"
docker logs be --since 24h 2>&1 | gzip > "$BK_DIR/applog-$TS.gz" || true

echo "[4/4] S3 업로드 (백업 전용 writer 키 — 앱 S3 키와 분리)"
BACKUP_BUCKET="${BACKUP_BUCKET:-dolog-prod-backup}"
if command -v aws >/dev/null 2>&1 && [ -n "${BACKUP_AWS_ACCESS_KEY_ID:-}" ]; then
  up() { AWS_ACCESS_KEY_ID="$BACKUP_AWS_ACCESS_KEY_ID" AWS_SECRET_ACCESS_KEY="$BACKUP_AWS_SECRET_ACCESS_KEY" \
    AWS_DEFAULT_REGION="${AWS_REGION:-ap-northeast-2}" aws s3 cp "$1" "s3://$BACKUP_BUCKET/$2" --only-show-errors; }
  up "$DUMP" "db/dolog-$TS.sql.gz"
  up "$BK_DIR/applog-$TS.gz" "log/applog-$TS.gz"
  [ "$(date +%u)" = 7 ] && up "$DUMP" "weekly/dolog-$TS.sql.gz"   # 일요일분 주간 보존(수명주기 8주)
  echo "  S3 업로드 완료 → s3://$BACKUP_BUCKET"
else
  echo "  WARN: aws CLI/BACKUP_AWS_* 없음 — 원격 업로드 스킵 (로컬 백업만)."
fi

# 로컬 3일 초과분 정리 (원격 보존은 버킷 수명주기 규칙이 담당)
find "$BK_DIR" -name '*.gz' -mtime +3 -delete
echo "OK: $DUMP 완료"
