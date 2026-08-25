#!/usr/bin/env bash
#
# Собирает архив и создаёт новую версию функции ydb-tickets.
#
# Использование:
#   ./deploy-ydb-tickets.sh                      # собрать и задеплоить
#   ./deploy-ydb-tickets.sh --no-build rag-embed   # задеплоить ез пересборки архива

set -euo pipefail

YC="${YC:-$HOME/yandex-cloud/bin/yc}"
SA_NAME="${SA_NAME:-ai-studio-sa}"

cd "$(dirname "$0")/.."

echo "==> параметры окружения"
SA_ID=$("$YC" iam service-account get "$SA_NAME" --format json \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])')
FOLDER_ID=$("$YC" config get folder-id)
printf '    аккаунт: %s\n    folder:  %s\n' "$SA_ID" "$FOLDER_ID"

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

name="ydb-tickets"
entrypoint="ru.hexlet.llm.developer425.ticket.YdbTicketsHandler"
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
    --secret environment-variable=YDB_ENDPOINT,name=ydb-endpoint,key=YDB_ENDPOINT \
    --secret environment-variable=YDB_DATABASE,name=ydb-database,key=YDB_DATABASE \
    | python3 -c 'import sys, json; v=json.load(sys.stdin); print("    версия:", v["id"])'