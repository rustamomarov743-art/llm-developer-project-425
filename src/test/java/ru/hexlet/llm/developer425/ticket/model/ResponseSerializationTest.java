package ru.hexlet.llm.developer425.ticket.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.junit.jupiter.api.Test;
import ru.hexlet.llm.developer425.core.Json;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверяет контракт сериализации ответов функции ydb-tickets: имена ключей в snake_case,
 * состав полей и формат времени. Тесты намеренно используют боевой
 * {@code YdbTicketsHandler.MAPPER}: иначе проверялось бы поведение, которого в проде нет.
 */
class ResponseSerializationTest {

    private static final String TICKET_ID = "0d1c8f4e-6b2a-4d55-9c31-7f0e2a5b8d10";
    private static final String MESSAGE_ID = "7f0e2a5b-8d10-4c31-9d55-0d1c8f4e6b2a";

    private static final Instant CREATED_AT = Instant.parse("2026-08-26T10:15:30Z");

    /** То же время в том виде, в каком оно попадает в JSON. */
    private static final String CREATED_AT_JSON = "2026-08-26T10:15:30Z";
    private static final ObjectMapper MAPPER = Json.mapper();

    @Test
    void ticketResponseKeepsSnakeCaseKeys() throws Exception {
        TicketResponse response =
                new TicketResponse(TICKET_ID, "open", "bug", "письма не приходят", CREATED_AT);

        JsonNode json = toJson(response);

        assertEquals(Set.of("id", "status", "category", "text", "created_at"), keysOf(json));
        assertEquals(TICKET_ID, json.get("id").asText());
        assertEquals("open", json.get("status").asText());
        assertEquals("bug", json.get("category").asText());
        assertEquals("письма не приходят", json.get("text").asText());
        assertIsoInstant(CREATED_AT_JSON, json.get("created_at"));
    }

    @Test
    void ticketResponseSurvivesRoundTrip() throws Exception {
        TicketResponse response =
                new TicketResponse(TICKET_ID, "open", "bug", "письма не приходят", CREATED_AT);

        String json = MAPPER.writeValueAsString(response);

        assertEquals(response, MAPPER.readValue(json, TicketResponse.class));
    }

    /**
     * У {@code TicketResponse} нет проверок в конструкторе, поэтому пустые поля не падают,
     * а уходят наружу как {@code null}. Тест фиксирует это как осознанное поведение.
     */
    @Test
    void ticketResponseWritesNullsAsJsonNull() throws Exception {
        JsonNode json = toJson(new TicketResponse(null, null, null, null, null));

        assertEquals(Set.of("id", "status", "category", "text", "created_at"), keysOf(json));
        assertTrue(json.get("id").isNull());
        assertTrue(json.get("created_at").isNull());
    }

    @Test
    void createTicketResponseKeepsSnakeCaseKeys() throws Exception {
        JsonNode json = toJson(new CreateTicketResponse(TICKET_ID, CREATED_AT));

        assertEquals(Set.of("ticket_id", "created_at"), keysOf(json));
        assertEquals(TICKET_ID, json.get("ticket_id").asText());
        assertIsoInstant(CREATED_AT_JSON, json.get("created_at"));
    }

    /**
     * Фиксирует формат времени: {@code WRITE_DATES_AS_TIMESTAMPS} отключён, поэтому
     * {@code Instant} уходит строкой ISO-8601, а не числом epoch-секунд. Наносекунды при этом
     * сохраняются полностью.
     */
    @Test
    void writesInstantAsIsoString() throws Exception {
        Instant withNanos = Instant.parse("2026-08-26T10:15:30.123456789Z");

        JsonNode json = toJson(new CreateTicketResponse(TICKET_ID, withNanos));

        assertIsoInstant("2026-08-26T10:15:30.123456789Z", json.get("created_at"));
    }

    @Test
    void createTicketResponseSurvivesRoundTrip() throws Exception {
        CreateTicketResponse response = new CreateTicketResponse(TICKET_ID, CREATED_AT);

        String json = MAPPER.writeValueAsString(response);

        assertEquals(response, MAPPER.readValue(json, CreateTicketResponse.class));
    }

    /** {@code Objects.requireNonNull} Jackson заворачивает в свою ошибку. */
    @Test
    void createTicketResponseRejectsMissingTicketId() {
        String withoutTicketId = """
                {"created_at": "2026-08-26T10:15:30Z"}
                """;

        assertThrows(ValueInstantiationException.class,
                () -> MAPPER.readValue(withoutTicketId, CreateTicketResponse.class));
    }

    @Test
    void appendMessageResponseKeepsSnakeCaseKeys() throws Exception {
        JsonNode json = toJson(new AppendMessageResponse(MESSAGE_ID, true));

        assertEquals(Set.of("message_id", "ok"), keysOf(json));
        assertEquals(MESSAGE_ID, json.get("message_id").asText());
        assertTrue(json.get("ok").isBoolean());
        assertTrue(json.get("ok").asBoolean());
    }

    @Test
    void appendMessageResponseWritesFalseFlag() throws Exception {
        JsonNode json = toJson(new AppendMessageResponse(MESSAGE_ID, false));

        assertEquals(Set.of("message_id", "ok"), keysOf(json));
        assertTrue(json.get("ok").isBoolean());
        assertFalse(json.get("ok").asBoolean());
    }

    @Test
    void appendMessageResponseSurvivesRoundTrip() throws Exception {
        AppendMessageResponse response = new AppendMessageResponse(MESSAGE_ID, true);

        String json = MAPPER.writeValueAsString(response);

        assertEquals(response, MAPPER.readValue(json, AppendMessageResponse.class));
    }

    @Test
    void appendMessageResponseRejectsMissingMessageId() {
        String withoutMessageId = """
                {"ok": true}
                """;

        assertThrows(ValueInstantiationException.class,
                () -> MAPPER.readValue(withoutMessageId, AppendMessageResponse.class));
    }

    private static void assertIsoInstant(String expected, JsonNode node) {
        assertTrue(node.isTextual(),
                () -> "created_at должен быть строкой, а не " + node.getNodeType());
        assertEquals(expected, node.asText());
        assertEquals(Instant.parse(expected), Instant.parse(node.asText()));
    }

    private static JsonNode toJson(Object response) throws Exception {
        return MAPPER.readTree(MAPPER.writeValueAsString(response));
    }

    private static Set<String> keysOf(JsonNode json) {
        Set<String> keys = new LinkedHashSet<>();
        json.fieldNames().forEachRemaining(keys::add);
        return keys;
    }
}
