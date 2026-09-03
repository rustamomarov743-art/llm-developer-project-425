package ru.hexlet.llm.developer425.ticket;

import ru.hexlet.llm.developer425.core.Ydb;
import ru.hexlet.llm.developer425.ticket.model.TicketResponse;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TicketRepository {

    private static final String CREATE_TICKET ="""
            DECLARE $id AS Utf8;
            DECLARE $user_id AS Utf8;
            DECLARE $category AS Utf8;
            DECLARE $status AS Utf8;
            DECLARE $text AS Utf8;
            DECLARE $updated_at AS Timestamp;
            
            $input = (
                SELECT
                    $id AS id,
                    $user_id AS user_id,
                    $category AS category,
                    $status AS status,
                    $text AS text,
                    $updated_at AS updated_at
            );
            
            UPSERT INTO tickets (id, user_id, category, status, text, created_at, updated_at)
            SELECT
                i.id AS id,
                i.user_id AS user_id,
                i.category AS category,
                i.status AS status,
                i.text AS text,
                CASE
                    WHEN t.id IS NULL THEN i.updated_at
                    ELSE t.created_at
                END AS created_at,
                i.updated_at AS updated_at
            FROM $input AS i
            LEFT JOIN tickets AS t ON t.id = i.id;
            """;


    private static final String APPEND_MESSAGE ="""
            DECLARE $id AS Utf8;
            DECLARE $ticket_id AS Utf8;
            DECLARE $role AS Utf8;
            DECLARE $text AS Utf8;
            DECLARE $model AS Utf8;
            DECLARE $tokens_in AS Uint64;
            DECLARE $tokens_out AS Uint64;
            DECLARE $latency_ms AS Uint32;
            DECLARE $created_at AS Timestamp;
            
            REPLACE INTO messages (id,ticket_id,role,text,model,tokens_in,tokens_out,latency_ms,created_at)
            SELECT $id, $ticket_id, $role, $text, $model, $tokens_in, $tokens_out, $latency_ms, $created_at;
            """;


    private static final String FIND_TICKETS = """
            DECLARE $user_id AS Utf8;
            
            SELECT id,category,status,created_at,text
            FROM tickets
            WHERE user_id = $user_id;
            """;

    private TicketRepository() {
    }

    /**
     * Пишет заявку: новую создаёт, существующую обновляет по {@code id}. Момент {@code updatedAt}
     * при создании становится и {@code created_at} — так решает сам запрос {@code CREATE_TICKET}.
     */
    public static void saveTicket(String id, String userId, String category, String status,
                                  String text, Instant updatedAt) {
        Params params = Params.of(
                "$id", text(id, "id"),
                "$user_id", text(userId, "userId"),
                "$category", text(category, "category"),
                "$status", text(status, "status"),
                "$text", text(text, "text"),
                "$updated_at", timestamp(updatedAt, "updatedAt"));

        Ydb.execute(CREATE_TICKET, params);
    }

    /**
     * Добавляет сообщение в переписку по заявке. Поля {@code model}, {@code tokensIn},
     * {@code tokensOut} и {@code latencyMs} осмысленны только для ответов агента, но запрос
     * объявляет их обязательными — для сообщений пользователя передавайте пустую строку и нули.
     */
    public static void saveMessage(String id, String ticketId, String role, String text,
                                   String model, long tokensIn, long tokensOut, long latencyMs,
                                   Instant createdAt) {
        Params params = Params.of(
                "$id", text(id, "id"),
                "$ticket_id", text(ticketId, "ticketId"),
                "$role", text(role, "role"),
                "$text", text(text, "text"),
                "$model", text(model, "model"),
                "$tokens_in", PrimitiveValue.newUint64(tokensIn),
                "$tokens_out", PrimitiveValue.newUint64(tokensOut),
                "$latency_ms", PrimitiveValue.newUint32(latencyMs),
                "$created_at", timestamp(createdAt, "createdAt"));

        Ydb.execute(APPEND_MESSAGE, params);
    }

    /**
     * Возвращает заявки пользователя. Текст обращения остаётся пустым: {@code FIND_TICKETS}
     * его не выбирает, чтобы не тащить длинные тела писем в список.
     */
    public static List<TicketResponse> findTickets(String userId) {
        Params params = Params.of("$user_id", text(userId, "userId"));
        QueryReader queryReader = Ydb.read(FIND_TICKETS, params);

        List<TicketResponse> tickets = new ArrayList<>();
        for (ResultSetReader resultSet : queryReader) {
            while (resultSet.next()) {
                tickets.add(new TicketResponse(
                        readText(resultSet.getColumn("id")),
                        readText(resultSet.getColumn("status")),
                        readText(resultSet.getColumn("category")),
                        readText(resultSet.getColumn("text")),
                        readTimestamp(resultSet.getColumn("created_at"))));
            }
        }
        return List.copyOf(tickets);
    }

    private static PrimitiveValue text(String value, String name) {
        return PrimitiveValue.newText(Objects.requireNonNull(value, name + " is null"));
    }

    private static PrimitiveValue timestamp(Instant value, String name) {
        return PrimitiveValue.newTimestamp(Objects.requireNonNull(value, name + " is null"));
    }

    private static String readText(ValueReader column) {
        return isNull(column) ? null : column.getText();
    }

    private static Instant readTimestamp(ValueReader column) {
        return isNull(column) ? null : column.getTimestamp();
    }

    /**
     * Колонки в схеме объявлены без NOT NULL, поэтому YDB отдаёт их как Optional и внутри может
     * лежать NULL. Прямой {@code getText()} на таком значении бросает NullPointerException.
     */
    private static boolean isNull(ValueReader column) {
        return column.getType().getKind() == Type.Kind.OPTIONAL && !column.isOptionalItemPresent();
    }
}
