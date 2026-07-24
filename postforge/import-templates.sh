#!/usr/bin/env bash
#
# ScrumTools e-posta şablonlarını PostForge'a yükler (idempotent: varsa günceller).
#
# Kullanım (Git Bash / WSL / Linux):
#   PF_EMAIL=... PF_PASSWORD=... ./postforge/import-templates.sh
#   PF_TOKEN=<jwt> ./postforge/import-templates.sh
#
# Ortam değişkenleri:
#   PF_BASE_URL  PostForge adresi (varsayılan: https://postforge.kocak.net.tr)
#   PF_TOKEN     Hazır JWT — verilirse login atlanır
#   PF_EMAIL     PostForge kullanıcı e-postası (PF_TOKEN yoksa zorunlu)
#   PF_PASSWORD  PostForge şifresi (PF_TOKEN yoksa zorunlu)
#
set -euo pipefail

BASE_URL="${PF_BASE_URL:-https://postforge.kocak.net.tr}"
BASE_URL="${BASE_URL%/}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFEST="$DIR/templates.json"

command -v jq >/dev/null 2>&1 || { echo "HATA: jq kurulu değil (https://jqlang.github.io/jq/)" >&2; exit 1; }
[ -f "$MANIFEST" ] || { echo "HATA: $MANIFEST bulunamadı" >&2; exit 1; }

TOKEN="${PF_TOKEN:-}"
if [ -z "$TOKEN" ]; then
  : "${PF_EMAIL:?PF_EMAIL veya PF_TOKEN tanımlayın}"
  : "${PF_PASSWORD:?PF_PASSWORD veya PF_TOKEN tanımlayın}"
  echo "→ PostForge'a giriş yapılıyor ($BASE_URL)..."
  TOKEN=$(curl -fsS -X POST "$BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$(jq -n --arg e "$PF_EMAIL" --arg p "$PF_PASSWORD" '{email:$e,password:$p}')" \
    | jq -r '.token // empty')
  [ -n "$TOKEN" ] || { echo "HATA: giriş başarısız (e-posta/şifre?)" >&2; exit 1; }
fi

echo "→ Mevcut şablonlar okunuyor..."
EXISTING=$(curl -fsS "$BASE_URL/api/templates" -H "Authorization: Bearer $TOKEN")

total=$(jq 'length' "$MANIFEST")
for i in $(seq 0 $((total - 1))); do
  entry=$(jq ".[$i]" "$MANIFEST")
  code=$(jq -r '.code' <<<"$entry")
  file=$(jq -r '.file' <<<"$entry")

  [ -f "$DIR/$file" ] || { echo "HATA: $DIR/$file bulunamadı" >&2; exit 1; }

  body=$(jq -n \
    --arg code    "$code" \
    --arg name    "$(jq -r '.name' <<<"$entry")" \
    --arg subject "$(jq -r '.subjectTemplate' <<<"$entry")" \
    --rawfile html "$DIR/$file" \
    --arg schema  "$(jq -c '.paramsSchema' <<<"$entry")" \
    '{code:$code, name:$name, subjectTemplate:$subject, htmlBody:$html, paramsSchema:$schema, active:true}')

  id=$(jq -r --arg c "$code" 'map(select(.code == $c)) | .[0].id // empty' <<<"$EXISTING")

  if [ -n "$id" ]; then
    curl -fsS -o /dev/null -X PUT "$BASE_URL/api/templates/$id" \
      -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$body"
    echo "  ✓ güncellendi: $code (id=$id)"
  else
    curl -fsS -o /dev/null -X POST "$BASE_URL/api/templates" \
      -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$body"
    echo "  ✓ oluşturuldu: $code"
  fi
done

echo "Tamam — $total şablon senkronize edildi."
