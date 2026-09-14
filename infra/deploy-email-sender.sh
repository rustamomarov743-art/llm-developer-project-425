#!/usr/bin/env bash
#
# Собирает архив и создаёт новую версию функции deploy-email-sender.
#
# Использование:
#   ./deploy-email-sender.sh                       # собрать и задеплоить
#   ./deploy-email-sender.sh --no-build   # задеплоить ез пересборки архива

set -euo pipefail

cd "$(dirname "$0")/.."
source infra/lib/env.sh
require YC SA_NAME YC_FOLDER_ID OPERATOR_EMAIL HELPDESK_MAILBOX SMTP_HOST SMTP_PORT SMTP_USER

echo "==> параметры окружения"
SA_ID=$("$YC" iam service-account get "$SA_NAME" --format json \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])')
printf '    аккаунт: %s\n    folder:  %s\n' "$SA_ID" "$YC_FOLDER_ID"

BUILD=true
if [[ "${1:-}" == "--no-build" ]]; then
    BUILD=false
    shift
fi

[[ -x "$YC" ]] || { echo "не найден: $YC" >&2; exit 1; }

if [[ "$BUILD" == true ]]; then
    ./package.sh
fi

ARCHIVE=target/help-desc.zip
[[ -r "$ARCHIVE" ]] || { echo "нет архива $ARCHIVE — соберите проект" >&2; exit 1; }

name="email-sender"
entrypoint="ru.hexlet.llm.developer425.mail.EmailSender"
memory="256m"
timeout="120s"

echo "==> $name ($entrypoint, $memory, $timeout)"

if ! "$YC" serverless function get "$name" >/dev/null 2>&1; then
    echo "    функции нет, создаю"
    "$YC" serverless function create --name "$name" >/dev/null
fi

"$YC" serverless function version create \
    --function-name "$name" --runtime java21 \
    --entrypoint "$entrypoint" \
    --memory "$memory" --execution-timeout "$timeout" \
    --service-account-id "$SA_ID" \
    --source-path "$ARCHIVE" \
    --format json \
    --environment "$(env_list YC_FOLDER_ID OPERATOR_EMAIL SMTP_HOST SMTP_PORT SMTP_USER HELPDESK_MAILBOX)" \
    --secret environment-variable=SMTP_PASSWORD,name=email-credentials,key=password \
    | python3 -c 'import sys, json; v=json.load(sys.stdin); print("    версия:", v["id"])'