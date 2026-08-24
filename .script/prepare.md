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
## Создать таблицу bot_state в БД для хранения текущего состояние бота
```
CREATE TABLE bot_state (
  key           Utf8,        -- Ключ
  value         Utf8,        -- значение
  PRIMARY KEY (key)
);
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
    ydb.editor
```bash
SA_ID=$(yc iam service-account get --name ai-studio-sa --format json | jq -r .id)
FOLDER_ID=$(yc config get folder-id)
for ROLE in \
    functions.functionInvoker \
    serverless.mcpGateways.invoker \
    lockbox.payloadViewer \
    ai.languageModels.user \
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

Промт:
    
    Ты — ассистент поддержки. Ты общаешься с пользователем системы и помогаешь решить его вопросы. Для получения
    информации используй инструмент поиска по документации. Отвечай строго на основе предоставленной базы знаний. Если
    в базе знаний нет информации для ответа, прямо скажи пользователю: «К сожалению, в базе знаний нет ответа на этот
    вопрос. Я могу передать запрос специалисту». Отвечай кратко, понятно и по делу. Если вопрос не относится к
    поддержке, вежливо откажись отвечать.

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
## Создать таблицы tickets и messages 
см. [schema.sql](../help-desc-proj/src/main/resources/ydb_tickets/schema.sql)
