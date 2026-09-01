Углубление workflow: авто-эскалация тикетов в YDB

Превращаем простой «агент отвечает на обращение» в осмысленную цепочку по расписанию: раз в день находим открытые тикеты, которые давно ждут ответа, → агент формирует дайджест для оператора → дайджест отправляется оператору по почте. Заодно осваиваем ретраи, обработку ошибок и трассировку шагов.

Pull-режим workflow (по расписанию — раз в день или раз в час для демо). Источник данных — YDB из шага про MCP и YDB:

```
Timer (cron 09:00)
│
▼
Workflow
├─ databaseQuery  → открытые тикеты старше 24 часов (YQL SELECT)
├─ switch         → если просроченных нет, выйти в терминальный шаг
├─ aiStudioAgent  → дайджест по выборке для оператора
├─ databaseQuery  → пометить выбранные тикеты как escalated (YQL UPDATE)
└─ httpCall (POST) → отправить дайджест оператору по почте (через CF email-sender)
```

Шаги с внешними вызовами (databaseQuery, aiStudioAgent, httpCall) стоит снабдить retryPolicy на 5xx/timeout — тогда разовая ошибка YDB или AI Studio не роняет workflow.
Ссылки

    Шаг AIStudioAgent в Yandex Workflows — инструкция от Яндекс
    Шаг DatabaseQuery (SQL-запросы к YDB) в Yandex Workflows — инструкция от Яндекс
    Формат cron-выражения — таблица шести полей и правило про ?
    JSON-схема YaWL — машиночитаемая спецификация: полный список полей каждого шага, коды ошибок для retryPolicy, границы значений. Ей же можно проверить свой workflow.yaml до деплоя

Задачи

    Опишите workflow.yaml в формате YaWL 0.1. Это не --file workflow.yaml и не deploy-revision — используется yc serverless workflow create --yaml-spec. Шаги в YaWL имеют camelCase-имена (databaseQuery, aiStudioAgent, httpCall, switch, success) и next живёт внутри шага, а не снаружи. Шаблоны — jq-интерполяция '\(...)', не ${...} (последнее даёт невнятное Internal error).
        databaseQuery — SQL-запрос к YDB/Postgres/MySQL/ClickHouse. Документация: https://yandex.cloud/ru/docs/serverless-integrations/concepts/workflows/yawl/integration/databasequery.
        aiStudioAgent — вызов агента из Agent Atelier (тот же agent_id, что на шаге про настройку AI Studio). Документация: https://yandex.cloud/ru/docs/serverless-integrations/concepts/workflows/yawl/integration/aistudioagent.

    Граф шагов соберите сами. Требуемая последовательность:
        databaseQuery — выбрать открытые тикеты старше порога (24 часа);
        switch — если просроченных нет, уйти в терминальный шаг (у switch нет default — добавьте catch-all condition: "true");
        aiStudioAgent — сформировать дайджест по выборке (тот же agent_id, что на шаге про настройку AI Studio);
        databaseQuery — пометить выбранные тикеты как escalated (YQL UPDATE с тем же условием, что и SELECT), чтобы завтра они не попали в дайджест повторно;
        httpCall — отправить дайджест оператору по почте через CF email-sender (в Python-примерах — src/email_sender.py); YaWL напрямую SMTP не умеет, поэтому HTTP-вызов на функцию-обёртку. Адрес оператора функция берёт из своего окружения (OPERATOR_EMAIL), а не из тела запроса — почему так, в «Подсказках».

    Форма YaWL (dummy-пример — показывает только синтаксис, не бизнес-логику):

    yawl: "0.1"
    start: step_one
    steps:
      step_one:
        databaseQuery:
          connection:                  # подключение к YDB — обязательный блок
            type: YDB
            host: ydb.serverless.yandexcloud.net
            port: 2135
            database: /ru-central1/<cloud-id>/<db-id>
            ssl: true
            iam: true                  # workflow ходит в YDB от своего SA
          query: "SELECT 1"            # реальный SQL — ваш
          mode: QUERY
          next: check
      check:
        switch:
          choices:
            - condition: "true"        # catch-all, поля `default` НЕТ
              next: done
      done:
        success: {}

    Назначьте SA workflow'а роли: ydb.editor, ai.assistants.editor, ai.languageModels.user. Без ai.assistants.editor шаг aiStudioAgent падает с 403 Forbidden на https://ai.api.cloud.yandex.net/v1/responses. Команда — по аналогии с шагом про настройку AI Studio (одна роль за вызов):

    for ROLE in ydb.editor ai.assistants.editor ai.languageModels.user; do
      yc resource-manager folder add-access-binding \
        --id <folder-id> --service-account-id <SA_ID> --role "$ROLE"
    done

    Создайте workflow:

    yc serverless workflow create \
      --name daily-escalation \
      --yaml-spec src/workflow.yaml \
      --service-account-id <SA_ID>

    Обновление через yc serverless workflow update --yaml-spec ....

    Запланируйте расписание (cron из 6 полей min hour dom mon dow year; timezone отдельно). Поля dom и dow нельзя заполнить одновременно — в одном из них ставится ?, поэтому «каждый день в 9:00» записывается со знаком вопроса в дне недели:

    yc serverless workflow update \
      --name daily-escalation \
      --yaml-spec src/workflow.yaml \
      --schedule-cron-expression "0 9 * * ? *" \
      --schedule-timezone Europe/Moscow

    Важно: после назначения расписания scheduled-trigger запускает workflow от имени своего SA. Если SA триггера совпадает с SA workflow'а (как у нас — ai-studio-sa), всё равно нужны две роли на workflow. Без них каскад ошибок:
        нет serverless.workflows.executor → service account does not have rights to start the workflow (или 400 can't invoke workflow)
        нет serverless.workflows.viewer → 400 doesn't have get permission for workflow (API делает GET перед запуском, поэтому viewer нужен даже для invoke)

    Рабочий способ — выдать обе роли SA workflow'а:

    yc serverless workflow add-access-binding \
      --name daily-escalation \
      --role serverless.workflows.executor \
      --service-account-id <SA_ID>
    yc serverless workflow add-access-binding \
      --name daily-escalation \
      --role serverless.workflows.viewer \
      --service-account-id <SA_ID>

    Проверка: yc serverless workflow list-access-bindings --name daily-escalation — должна показать обе строки.

    yc serverless workflow allow-unauthenticated-execution формально существует, но в текущем YC не сохраняется (флаг executionPermissions остаётся пустым) — поэтому не полагайтесь на него.

    Ручной тест (до расписания): поставьте порог в Interval('PT1H') вместо 'PT24H', чтобы существующие тикеты попали в выборку; запустите yc serverless workflow execution start daily-escalation; проверьте execution get и приход письма оператору. Потом верните 'PT24H'.

    Умышленно сломайте секрет/роль → проверьте, что шаг падает с понятной ошибкой (например STEP_PERMISSION_DENIED если забыть роль), в execution get видны и error_code, и сообщение.

Подсказки

    В YDB SQL нет CurrentTimestamp() — используйте CurrentUtcTimestamp().
    Шаг aiStudioAgent принимает поля promptTemplateId, message, autoApprove (не agentId/prompts; имена полей в YaWL camelCase, auto_approve схема не примет); терминальные шаги — success: {} и fail: { errorMessage: "..." }.
    Дайджест оператору шлём через CF email-sender: httpCall с method: POST и JSON-телом {subject, body}. YaWL сырой SMTP не умеет — функция-обёртка отправляет письмо по SMTP за workflow (в Python-варианте — smtplib.SMTP_SSL).
    Адресата функция берёт из OPERATOR_EMAIL, а не из запроса, и это не придирка к стилю. httpCall не шлёт IAM-токен, поэтому функцию приходится открывать без аутентификации — значит дёрнуть её может любой, кто узнал URL. Пока адресат приходит в теле, это рассылка с корпоративного ящика на любой адрес; с адресатом из окружения худшее, что можно получить, — лишний дайджест оператору.
    Ретраи имеют смысл только на 5xx/timeout: STEP_PERMISSION_DENIED ими не лечится — это не временная ошибка, и повтор лишь оттянет понятный отказ по правам.
    Форма политики: retryPolicy — поле внутри тела шага, рядом с next и timeout; defaultRetryPolicy на уровне процесса задаёт её сразу всем шагам. Обязательное поле одно — errorList, коды берутся из перечисления в схеме (STEP_TIMEOUT, HTTP_CALL_503, DATABASE_QUERY_UNAVAILABLE и так далее), режим выбора задаёт errorListMode: INCLUDE или EXCLUDE. Задержки пишутся только в секундах (initialDelay: 2s, maxDelay: 30s) — 1m и 1h схема отклонит; retryCount не больше 100.

Результат шага

    Workflow отрабатывает по расписанию; оператору на OPERATOR_EMAIL приходит письмо-дайджест по просроченным тикетам.
    В YDB обработанные тикеты переходят в статус escalated (видно через SELECT status, count(*) FROM tickets GROUP BY status) — повторно в дайджест они не попадают.
    В трейсах YC Logging видно каждый шаг отдельно (вход, выход, длительность, токены).
    Вы можете объяснить, чем pull-режим poller'а (CF по таймеру, шаг про email-workflow) отличается от workflow по расписанию (этот шаг) и когда какой подход применять.
