#!/usr/bin/env bash
#
# Обновляет workflow daily-escalation
#
#   ./deploy-daily-escalation-workflow.sh            # обновить workflow
#   ./deploy-daily-escalation-workflow.sh --render   # только показать готовую спецификацию, без облака
#
# ID функций подставляются здесь и только здесь: в workflow/daily-escalation.yaml лежат плейсхолдеры.
#
set -euo pipefail

YC="${YC:-$HOME/yandex-cloud/bin/yc}"
WORKFLOW_NAME="${WORKFLOW_NAME:-daily-escalation}"
SA_NAME="${SA_NAME:-ai-studio-sa}"
AGENT_ID="${AGENT_ID:-fvtdutb2q552omlr99sq}"
CRON="${CRON:-0 0 9 * * *}"
DB_NAME="${DB_NAME:-help-desk-db}"

cd "$(dirname "$0")/.."

RENDER_ONLY=false
for arg in "$@"; do
    case "$arg" in
        --render) RENDER_ONLY=true ;;
        *) echo "неизвестный флаг: $arg" >&2; exit 1 ;;
    esac
done

[[ -x "$YC" ]] || { echo "не найден: $YC" >&2; exit 1; }

function_id() {
    "$YC" serverless function get "$1" --format json \
        | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])' 2>/dev/null \
        || { echo "не найдена функция $1" >&2; exit 1; }
}

# Путь базы отдельным полем не приходит: он зашит в endpoint query-параметром
# ?database=/ru-central1/<cloud>/<db> — оттуда и достаём.
database_path() {
    "$YC" ydb database get "$1" --format json \
        | python3 -c 'import sys, json, urllib.parse as u; print(u.parse_qs(u.urlparse(json.load(sys.stdin)["endpoint"]).query)["database"][0])' \
        || { echo "не найдена база $1 (переопределяется через DB_NAME)" >&2; exit 1; }
}

echo "==> база"
DATABASE="${DATABASE:-$(database_path "$DB_NAME")}"

echo "DATABASE ==> $DATABASE"
echo "AGENT_ID ==> $AGENT_ID"

echo "==> ID функций"
EMAIL_SENDER_ID=$(function_id email-sender)

printf '    email-sender:              %s \n' "$EMAIL_SENDER_ID"

RENDERED=workflow/target/daily-escalation.yaml
mkdir -p workflow/target
sed \
    -e "s|__EMAIL_SENDER_ID__|$EMAIL_SENDER_ID|" \
    -e "s|__DATABASE__|$DATABASE|" \
    -e "s|__AGENT_ID__|$AGENT_ID|" \
    workflow/daily-escalation.yaml > "$RENDERED"

if grep -q '__[A-Z_]*__' "$RENDERED"; then
    echo "в спецификации остались неподставленные плейсхолдеры:" >&2
    grep -n '__[A-Z_]*__' "$RENDERED" >&2
    exit 1
fi

echo "==> спецификация: $RENDERED"
if [[ "$RENDER_ONLY" == true ]]; then
    cat "$RENDERED"
    exit 0
fi

SA_ID=$("$YC" iam service-account get "$SA_NAME" --format json \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])')
echo "    аккаунт шлюза: $SA_ID"

if "$YC" serverless workflow get "$WORKFLOW_NAME" >/dev/null 2>&1; then
    echo "==> workflow существует, обновляю"
    ACTION=update
else
    echo "==> создаю workflow"
    ACTION=create
fi

"$YC" serverless workflow "$ACTION" \
    --name "$WORKFLOW_NAME" \
    --yaml-spec "$RENDERED" \
    --service-account-id "$SA_ID" \
    --schedule-cron-expression "$CRON" \
    --schedule-timezone Europe/Moscow \
    --format json \
    | python3 -c '
import sys, json
gw = json.load(sys.stdin)
print("    id:          ", gw.get("id"))
print("    статус:      ", gw.get("status"))
print("    имя:         ", gw.get("name"))
'
