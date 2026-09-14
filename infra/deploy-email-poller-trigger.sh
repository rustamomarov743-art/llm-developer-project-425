#!/usr/bin/env bash

set -euo pipefail

YC="${YC:-$HOME/yandex-cloud/bin/yc}"
SA_NAME="${SA_NAME:-ai-studio-sa}"

echo "==> параметры окружения"
SA_ID=$("$YC" iam service-account get "$SA_NAME" --format json \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])')
FOLDER_ID=$("$YC" config get folder-id)
printf '    аккаунт: %s\n    folder:  %s\n' "$SA_ID" "$FOLDER_ID"

"$YC" serverless trigger create timer \
  --name email-poller-trigger \
  --cron-expression "0/1 * * * ? *" \
  --invoke-function-name email-poller \
  --invoke-function-tag '$latest' \
  --invoke-function-service-account-id "$SA_ID"