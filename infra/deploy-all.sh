#!/usr/bin/env bash
#
# Разворачивает все компоненты по порядку зависимостей.
#
#   ./infra/deploy-all.sh              # собрать архив один раз и задеплоить всё
#   ./infra/deploy-all.sh --no-build   # задеплоить уже собранный target/help-desc.zip
#   ./infra/deploy-all.sh --kb         # заодно пересоздать vector store из docs/*.md
#
# Повторный запуск безопасен: vector store создаётся, только если YC_VECTOR_STORE_ID
# пуст или передан --kb; остальные компоненты создаются или обновляются.
#
set -euo pipefail

cd "$(dirname "$0")/.."
source infra/lib/env.sh

BUILD=true
KB=false
for arg in "$@"; do
    case "$arg" in
        --no-build) BUILD=false ;;
        --kb) KB=true ;;
        *) echo "неизвестный флаг: $arg" >&2; exit 1 ;;
    esac
done

# Проверяем всё сразу, чтобы не упасть на середине развёртывания.
require YC SA_NAME DB_NAME GATEWAY_NAME WORKFLOW_NAME WORKFLOW_CRON YC_FOLDER_ID YC_AGENT_ID \
    HELPDESK_MAILBOX IMAP_HOST IMAP_PORT IMAP_USER SMTP_HOST SMTP_PORT SMTP_USER OPERATOR_EMAIL

step() {
    echo
    echo "######## $1"
}

trap 'echo "развёртывание остановлено на шаге: ${CURRENT_STEP:-подготовка}" >&2' ERR

if [[ "$BUILD" == true ]]; then
    CURRENT_STEP="сборка"; step "$CURRENT_STEP"
    ./package.sh
fi

CURRENT_STEP="1/7 CF ydb-tickets"; step "$CURRENT_STEP"
./infra/deploy-ydb-tickets.sh --no-build

CURRENT_STEP="2/7 MCP-шлюз $GATEWAY_NAME"; step "$CURRENT_STEP"
./infra/deploy-ydb-tickets-mcp.sh

CURRENT_STEP="3/7 vector store"; step "$CURRENT_STEP"
if [[ "$KB" == true || -z "${YC_VECTOR_STORE_ID:-}" ]]; then
    ./infra/deploy-help-desc-kb.sh
    echo
    echo "впишите ID созданного vector store в .env (YC_VECTOR_STORE_ID) и запустите снова с --no-build"
    exit 0
fi
echo "пропускаю: YC_VECTOR_STORE_ID=$YC_VECTOR_STORE_ID (пересоздать — флаг --kb)"

CURRENT_STEP="4/7 CF email-sender"; step "$CURRENT_STEP"
./infra/deploy-email-sender.sh --no-build

CURRENT_STEP="5/7 CF email-poller"; step "$CURRENT_STEP"
./infra/deploy-email-poller.sh --no-build

CURRENT_STEP="6/7 таймер поллера"; step "$CURRENT_STEP"
./infra/deploy-email-poller-trigger.sh

CURRENT_STEP="7/7 workflow $WORKFLOW_NAME"; step "$CURRENT_STEP"
./infra/deploy-daily-escalation-workflow.sh

echo
echo "готово"
