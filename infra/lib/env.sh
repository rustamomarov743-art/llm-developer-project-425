# Общие функции deploy-скриптов. Подключается из корня репозитория:
#
#   source infra/lib/env.sh
#
# Загружает .env: переменная, заданная при запуске (`YC_AGENT_ID=... ./infra/deploy-...`),
# приоритетнее значения из файла. Значений по умолчанию нет — всё задаётся в .env.

ENV_FILE=.env

[[ -r "$ENV_FILE" ]] || { echo "нет $ENV_FILE: скопируйте .env.example в .env и заполните" >&2; exit 1; }

while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)= ]] || continue
    key="${BASH_REMATCH[1]}"
    [[ -z "${!key+set}" ]] || continue
    eval "export $line"
done < "$ENV_FILE"
unset line key

# Останавливает скрипт, если хотя бы одна из переменных пуста, и перечисляет все пустые.
require() {
    local name missing=()
    for name in "$@"; do
        [[ -n "${!name:-}" ]] || missing+=("$name")
    done
    if (( ${#missing[@]} > 0 )); then
        echo "не заданы в $ENV_FILE: ${missing[*]}" >&2
        exit 1
    fi
}

# Собирает значение для `yc ... --environment` из перечисленных переменных. Пустые
# пропускает: иначе функция получит "" вместо своего значения по умолчанию.
env_list() {
    local name pairs=()
    for name in "$@"; do
        if [[ -n "${!name:-}" ]]; then
            pairs+=("$name=${!name}")
        fi
    done
    local IFS=,
    echo "${pairs[*]}"
}
