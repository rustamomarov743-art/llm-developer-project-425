#!/usr/bin/env bash
#
# Собирает архив и создаёт новую версию функции deploy-email-poller.
#
# Использование:
#   ./deploy-email-poller.sh                       # собрать и задеплоить
#   ./deploy-email-poller.sh --no-build   # задеплоить ез пересборки архива

set -euo pipefail

cd "$(dirname "$0")/.."
source infra/lib/env.sh
require YC SA_NAME GATEWAY_NAME YC_FOLDER_ID YC_AGENT_ID YC_VECTOR_STORE_ID \
    HELPDESK_MAILBOX IMAP_HOST IMAP_PORT IMAP_USER SMTP_HOST SMTP_PORT SMTP_USER

echo "==> параметры окружения"
SA_ID=$("$YC" iam service-account get "$SA_NAME" --format json \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])')
# Адрес шлюза можно задать в .env; если пусто — берём домен шлюза GATEWAY_NAME.
if [[ -z "${YC_YDB_TICKETS_MCP_SERVER_URL:-}" ]]; then
    YC_YDB_TICKETS_MCP_SERVER_URL=$("$YC" serverless mcp-gateway get --name "$GATEWAY_NAME" --format json 2>/dev/null \
        | python3 -c 'import sys, json; print(json.load(sys.stdin)["base_domain"])' 2>/dev/null) \
        || { echo "не найден шлюз $GATEWAY_NAME — задеплойте его или задайте YC_YDB_TICKETS_MCP_SERVER_URL" >&2; exit 1; }
fi
printf '    аккаунт: %s\n    folder:  %s\n    mcp:     %s\n' "$SA_ID" "$YC_FOLDER_ID" "$YC_YDB_TICKETS_MCP_SERVER_URL"

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

name="email-poller"
entrypoint="ru.hexlet.llm.developer425.mail.EmailPoller"
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
    --environment "$(env_list YC_FOLDER_ID YC_VECTOR_STORE_ID YC_AGENT_ID YC_YDB_TICKETS_MCP_SERVER_URL \
        YC_GUARD_MODEL EMAIL_BATCH IMAP_HOST IMAP_PORT IMAP_USER SMTP_HOST SMTP_PORT SMTP_USER HELPDESK_MAILBOX)" \
    --secret environment-variable=IMAP_PASSWORD,name=email-credentials,key=password \
    --secret environment-variable=SMTP_PASSWORD,name=email-credentials,key=password \
    --secret environment-variable=YDB_ENDPOINT,name=ydb-endpoint,key=YDB_ENDPOINT \
    --secret environment-variable=YDB_DATABASE,name=ydb-database,key=YDB_DATABASE \
    | python3 -c 'import sys, json; v=json.load(sys.stdin); print("    версия:", v["id"])'