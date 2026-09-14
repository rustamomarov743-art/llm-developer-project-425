# Шаг 1
Установить YC - yandex console
# Шаг 2
## Создать БД
```bash
yc ydb database create help-desk-db --serverless
```
### Проверка
Статус RUNNING
```bash
yc ydb database get help-desk-db --format json | jq -r .status
```
Успешно выполняется 
```bash
yc serverless function list
```
## Получить секреты для LockBox
```bash
ENDPOINT=$(yc ydb database get help-desk-db --format json | jq -r .endpoint)
YDB_ENDPOINT=$(echo "$ENDPOINT" | sed -E 's|(grpcs://[^/?]+).*|\1|')
YDB_DATABASE=$(echo "$ENDPOINT" | sed -E 's|.*database=([^&]+).*|\1|')
echo "YDB_ENDPOINT=$YDB_ENDPOINT"
echo "YDB_DATABASE=$YDB_DATABASE"
```
# Шаг 3
## Создать сервисный аккаунт ai-studio-sa и назначить роли
### Сервисный аккаунт 
```bash
yc iam service-account create --name ai-studio-sa
```
### Назначить роли

    functions.functionInvoker 
    serverless.mcpGateways.invoker 
    lockbox.payloadViewer 
    ai.languageModels.user 
    ai.assistants.editor
    serverless.workflows.executor
    serverless.workflows.viewer
    ydb.editor
```bash
SA_ID=$(yc iam service-account get --name ai-studio-sa --format json | jq -r .id)
FOLDER_ID=$(yc config get folder-id)
for ROLE in \
    functions.functionInvoker \
    serverless.mcpGateways.invoker \
    lockbox.payloadViewer \
    ai.languageModels.user \
    ai.assistants.editor \
    serverless.workflows.executor \
    serverless.workflows.viewer \
    ydb.editor; do
  yc resource-manager folder add-access-binding \
    --id "$FOLDER_ID" \
    --service-account-id "$SA_ID" \
    --role "$ROLE"
done
```
### Проверка
```bash
FOLDER_ID=$(yc config get folder-id)
yc resource-manager folder list-access-bindings --id "$FOLDER_ID"
```
## Создать секреты в Lockbox
Создать секреты, которые получены на предыдущем шаге 
```bash
yc lockbox secret create \
  --name ydb-endpoint \
  --payload '[{"key":"YDB_ENDPOINT","text_value":"grpcs://...:2135"}]'
yc lockbox secret create \
  --name ydb-database \
  --payload '[{"key":"YDB_DATABASE","text_value":"/ru-central1/b1g.../etn..."}]'
```
## Создать агента help-desk в UI Agent
Имя: help-desk

Модель: yandexgpt

Промт: см. раздел [«Агент help-desk»](../README.md#агент-help-desk) в README.

# Шаг 4
## Создать почту, app-password и секрет
### Создать почту, app-password
    см. gmail.com
### Добавить app-password в lockbox
```bash
yc lockbox secret create \
  --name email-credentials \
  --payload '[{"key":"password","text_value":"..."}]'
```

# Шаг 5
## Создать таблицы tickets, messages и bot_state
см. [schema.sql](../src/ydb_tickets/schema.sql)
