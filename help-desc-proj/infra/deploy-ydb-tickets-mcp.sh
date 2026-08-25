#!/usr/bin/env bash
#
# Обновляет MCP-шлюз ydb-tickets-mcp
#
#   ./deploy-ydb-tickets-mcp.sh            # обновить шлюз
#   ./deploy-ydb-tickets-mcp.sh --render   # только показать готовую спецификацию, без облака
#
# ID функций подставляются здесь и только здесь: в mcp/ydb-tickets-mcp.yaml лежат плейсхолдеры.
#
set -euo pipefail

YC="${YC:-$HOME/yandex-cloud/bin/yc}"
GATEWAY_NAME="${GATEWAY_NAME:-ydb-tickets-mcp}"
SA_NAME="${SA_NAME:-ai-studio-sa}"

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

echo "==> ID функций"
YDB_TICKETS_ID=$(function_id ydb-tickets)

printf '    ydb-tickets:              %s \n' "$YDB_TICKETS_ID"

RENDERED=mcp/target/tools.rendered.yaml
mkdir -p mcp/target
sed \
    -e "s|__YDB_TICKETS_ID__|$YDB_TICKETS_ID|" \
    mcp/ydb-tickets-mcp.yaml > "$RENDERED"

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

if "$YC" serverless mcp-gateway get "$GATEWAY_NAME" >/dev/null 2>&1; then
    echo "==> шлюз существует, обновляю"
    ACTION=update
else
    echo "==> создаю шлюз"
    ACTION=create
fi

"$YC" serverless mcp-gateway "$ACTION" "$GATEWAY_NAME" \
    --tools-file "$RENDERED" \
    --service-account-id "$SA_ID" \
    --format json \
    | python3 -c '
import sys, json
gw = json.load(sys.stdin)
print("    id:          ", gw.get("id"))
print("    статус:      ", gw.get("status"))
print("    домен:       ", gw.get("base_domain") or gw.get("baseDomain"))
tools = gw.get("tools", [])
print("    инструментов:", len(tools))
for tool in tools:
    print("      -", tool.get("name"))
'
