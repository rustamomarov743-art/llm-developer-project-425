### Hexlet tests and linter status:
[![Actions Status](https://github.com/rustamomarov743-art/llm-developer-project-425/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/rustamomarov743-art/llm-developer-project-425/actions)

### Подготовительные скрипты
[prepare.md](.script/prepare.md)

Создан сервисный аккаунт ai-studio-sa с ролями
    
    functions.functionInvoker
    serverless.mcpGateways.invoker 
    lockbox.payloadViewer
    ai.languageModels.user
    ydb.editor

Секреты в Lockbox

| Имя           | имя параметра | значение        |
|---------------|---------------|-----------------|
| ydb-endpoint  | ydb-endpoint  | grpcs://...:... |
| ydb-database  | ydb-database  | /ru-central1/b1g.../etn...   |

