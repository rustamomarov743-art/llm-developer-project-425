#!/usr/bin/env bash
#
# Создаёт или обновляет таймер email-poller-trigger и снимает его с паузы.
#
#   ./deploy-email-poller-trigger.sh
#
set -euo pipefail

cd "$(dirname "$0")/.."
source infra/lib/env.sh
require YC SA_NAME YC_FOLDER_ID

TRIGGER_NAME="email-poller-trigger"
CRON="0/1 * * * ? *"

echo "==> параметры окружения"
SA_ID=$("$YC" iam service-account get "$SA_NAME" --format json \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])')
printf '    аккаунт: %s\n    folder:  %s\n' "$SA_ID" "$YC_FOLDER_ID"

json_field() {
    python3 -c 'import sys, json; print(json.load(sys.stdin).get(sys.argv[1], ""))' "$1"
}

# update и resume не находят триггер по --name (API получает пустой trigger_id), поэтому
# везде работаем по ID. Функцию для update тоже передаём по ID.
TRIGGER_ID=$("$YC" serverless trigger get "$TRIGGER_NAME" --format json 2>/dev/null | json_field id) \
    || TRIGGER_ID=""

if [[ -n "$TRIGGER_ID" ]]; then
    echo "==> триггер существует ($TRIGGER_ID), обновляю"
    FUNCTION_ID=$("$YC" serverless function get email-poller --format json | json_field id)
    "$YC" serverless trigger update timer --id "$TRIGGER_ID" \
        --new-cron-expression "$CRON" \
        --new-invoke-function-id "$FUNCTION_ID" \
        --new-invoke-function-tag '$latest' \
        --new-invoke-function-service-account-id "$SA_ID" \
        >/dev/null
else
    echo "==> создаю триггер"
    TRIGGER_ID=$("$YC" serverless trigger create timer \
        --name "$TRIGGER_NAME" \
        --cron-expression "$CRON" \
        --invoke-function-name email-poller \
        --invoke-function-tag '$latest' \
        --invoke-function-service-account-id "$SA_ID" \
        --format json | json_field id)
fi

# update не снимает паузу: без resume приостановленный поллер так и не заработает.
STATUS=$("$YC" serverless trigger get --id "$TRIGGER_ID" --format json | json_field status)
if [[ "$STATUS" == "PAUSED" ]]; then
    echo "==> триггер на паузе, возобновляю"
    "$YC" serverless trigger resume --id "$TRIGGER_ID" >/dev/null
    STATUS=$("$YC" serverless trigger get --id "$TRIGGER_ID" --format json | json_field status)
fi
echo "    статус: $STATUS"
