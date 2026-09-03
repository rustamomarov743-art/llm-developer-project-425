#!/usr/bin/env bash
#
# Собирает архив и создаёт новую версию функции deploy-email-poller.
#
# Использование:
#   ./deploy-email-poller.sh                       # собрать и задеплоить
#   ./deploy-email-poller.sh --no-build   # задеплоить ез пересборки архива

set -euo pipefail

YC="${YC:-$HOME/yandex-cloud/bin/yc}"
SA_NAME="${SA_NAME:-ai-studio-sa}"
GATEWAY_NAME="${GATEWAY_NAME:-ydb-tickets-mcp}"
MCP_SERVER_URL_DEFAULT="${MCP_SERVER_URL:-CHANGE_ME}"
AGENT_ID="${AGENT_ID:-fvtdutb2q552omlr99sq}"
VECTOR_STORE_ID="${VECTOR_STORE_ID:-fvtbrihvnea5jhfujree}"

cd "$(dirname "$0")/.."

echo "==> параметры окружения"
SA_ID=$("$YC" iam service-account get "$SA_NAME" --format json \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])')
FOLDER_ID=$("$YC" config get folder-id)
MCP_SERVER_URL=$("$YC" serverless mcp-gateway get --name "$GATEWAY_NAME" --format json 2>/dev/null \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["base_domain"])' 2>/dev/null) \
    || MCP_SERVER_URL=""
if [[ -z "$MCP_SERVER_URL" ]]; then
    MCP_SERVER_URL="$MCP_SERVER_URL_DEFAULT"
    echo "    шлюза $GATEWAY_NAME нет, беру значение по умолчанию"
fi
printf '    аккаунт: %s\n    folder:  %s\n    mcp:     %s\n' "$SA_ID" "$FOLDER_ID" "$MCP_SERVER_URL"

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
    --environment "YC_FOLDER_ID=$FOLDER_ID,YC_VECTOR_STORE_ID=$VECTOR_STORE_ID,YC_AGENT_ID=$AGENT_ID,YC_YDB_TICKETS_MCP_SERVER_URL=$MCP_SERVER_URL,IMAP_HOST=imap.gmail.com,IMAP_PORT=993,IMAP_USER=llm.developer.project.425@gmail.com,SMTP_HOST=smtp.gmail.com,SMTP_PORT=587,SMTP_USER=llm.developer.project.425@gmail.com,HELPDESK_MAILBOX=llm.developer.project.425@gmail.com" \
    --secret environment-variable=IMAP_PASSWORD,name=email-credentials,key=password \
    --secret environment-variable=SMTP_PASSWORD,name=email-credentials,key=password \
    --secret environment-variable=YDB_ENDPOINT,name=ydb-endpoint,key=YDB_ENDPOINT \
    --secret environment-variable=YDB_DATABASE,name=ydb-database,key=YDB_DATABASE \
    | python3 -c 'import sys, json; v=json.load(sys.stdin); print("    версия:", v["id"])'