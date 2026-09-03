# Аудит проекта против .my/step*.md

Дата: 2026-09-03. Проверка кода в help-desc-proj/ против требований шагов из .my/.

## Логирование (step4, step8, step9) — проверено, часть гэпов исправлена в коде

- OK: `GOT_UNSEEN=1`, `MSG num=... from=... subject=...` — есть в `MailProcessingService`.
- OK: сырой PII в логи не пишется (subject маскируется через `Pii.mask`, в `YdbTicketsHandler` логируется только `action`, без текста обращения).
- ИСПРАВЛЕНО: не было `AGENT_OK len=...` и `SEND_OK to=...` — требовались явно в «Результат шага» step4 и в чек-листе трейсов step9. Добавлены в `MailProcessingService.process(Message, Transport)` и `sendReply(...)`.
- ИСПРАВЛЕНО: `ContentType.OFF_TOPIC` не логировался и не отличался от `SAFE` — step8 требует «off-topic → создаётся, но логируется». Добавлена ветка `LOG.info("OFF_TOPIC detected")` без `return` (обработка продолжается).
- НЕ ИСПРАВЛЕНО (мягкое): маркер блокировки инъекции — `"INJECTION detected"`, в step8 в примере фигурирует `ALERT_INJECTION_BLOCKED`. Это в «Подсказках», не в «Результат шага» — не хардтребование, но если проверяющий будет grep'ать лог по этой строке, не найдёт.
- Также в этой сессии почти исправлена независимая проблема: `logback.xml` не подхватывался (в classpath не было движка Log4j2, реальный slf4j-биндинг — `logback-classic`; лишний `log4j2.xml` был мёртвым конфигом), плюс `package.sh` не копировал `src/main/resources` в архив деплоя — оба места починены.

## MCP-контракт (step5, discussion_step-4-5.md) — проверено, изменения НЕ вносились

- OK: JSON-контракт ответов (`ticket_id`/`created_at`, `message_id`/`ok`, `id`/`status`/`category`/`text`/`created_at`) совпадает со спекой буквально (через `@JsonProperty`).
- OK: `require_approval: "never"` выставлен в `AgentService`.
- OK: диспетчеризация по `action` через `@JsonTypeInfo` в `Event` корректно работает для direct invoke и MCP Hub call (обе формы плоские, с `action` на верхнем уровне).
- OK: `append-message` исключён из `allowedTools` агента (`AgentService.java:82` — только `create-ticket`, `list-my-tickets`). Это соответствует правке из `discussion_step-4-5.md` (Автор 2: «append-message не должен быть tool для llm») — сам `step5.md` в `.my/` этой правки текстуально не получил, но в коде она уже учтена.
- ГЭП: `mcp/ydb-tickets-mcp.yaml` всё ещё объявляет `append-message` как MCP-инструмент с описанием, рассчитанным на LLM («Идентификатор тикета берётся из ответа create-ticket»). Единственная защита от вызова агентом — client-side `allowedTools` в Java-коде, а не сам гейтвей. Если агент когда-нибудь пойдёт другим путём (сохранённый агент в UI, Workflow) без этого фильтра — tool снова станет виден LLM. Рекомендация: убрать `append-message` из `mcp-tools.yaml`, оставить там только 2 инструмента.
- ГЭП: `YdbTicketsHandler`/`Event.deserialize` не разворачивает конверт API Gateway (`{"httpMethod":"POST","body":"<JSON-строка>"}`) — обрабатываются только direct invoke и MCP Hub call. Явное требование step5 («три источника события») закрыто не полностью. На практике не стреляет: API Gateway нигде в `infra/` не создаётся и не используется.
- MINOR: `list-my-tickets` всегда возвращает `text: null` (осознанное упрощение в `TicketRepository.FIND_TICKETS`, есть поясняющий комментарий в коде), но формально расходится с примером ответа в step5, где `text` — содержательное поле.

## Где мы остановились

Проверили и частично исправили **логирование**, проверили (без исправлений) **MCP-контракт**. Дальше не смотрели:

- **PII/security (step8, помимо логирования)** — маскирование в БД, regex + LLM-классификатор инъекций, trusted/untrusted контекст в README.
- **Права и секреты (step3, step6)** — роли SA, состав секретов в Lockbox, роли SA у workflow.
- **step1** — общее описание (контекст, вряд ли требует проверки кода).
- **step2** — окружение, YDB, `.env`/`.env.example`.
- **step10** — финальный чек-лист сдачи (README.md, «что попробовать», ссылки).

## Что изменено в коде/репозитории за эту сессию (не только проверка)

- Пересобран корпус RAG в `help-desc-proj/docs/`: 01–10 дополнены конкретными фактами (сроки, лимиты, SLA), добавлен `11 — Администрирование офисного оборудования.md`; в `10` добавлено описание реальных категорий тикетов (`bug`/`docs`/`feature`/`access`).
- Черновик system prompt агента (описание инструментов `create-ticket`/`list-my-tickets`, категории, разведение «дайджест по своим тикетам» vs «дайджест по выборке для оператора») — передан пользователю текстом в чат для вставки в AI Studio; в репозитории не хранится, промпт живёт вне репо и привязан по `agent_id`.
- `src/main/resources/logback.xml` создан, `package.sh` дополнен копированием `src/main/resources` в архив деплоя.
- `MailProcessingService.java` дополнен логами `AGENT_OK`/`SEND_OK`/`OFF_TOPIC` (см. раздел «Логирование» выше).
