package ru.hexlet.llm.developer425.ticket.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Проверяет, что Jackson по полю {@code action} выбирает нужного наследника {@link Action}.
 * JSON в тестах повторяет то, что MCP-шлюз кладёт в тело запроса к функции ydb-tickets.
 */
class ActionDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesCreateTicket() throws Exception {
        String json = """
                {
                  "action": "create-ticket",
                  "user_id": "ru743@gmail.com",
                  "category": "bug",
                  "text": "Проверил спам — письма там тоже нет"
                }
                """;

        Action action = mapper.readValue(json, Action.class);

        Action.CreateTicket ticket = assertInstanceOf(Action.CreateTicket.class, action);
        assertEquals("create-ticket", ticket.getAction());
        assertEquals("ru743@gmail.com", ticket.getUserId());
        assertEquals("bug", ticket.getCategory());
        assertEquals("Проверил спам — письма там тоже нет", ticket.getText());
    }

    @Test
    void deserializesAppendMessage() throws Exception {
        String json = """
                {
                  "action": "append-message",
                  "ticket_id": "0d1c8f4e-6b2a-4d55-9c31-7f0e2a5b8d10",
                  "role": "user",
                  "text": "Не приходит письмо со ссылкой на сброс пароля"
                }
                """;

        Action action = mapper.readValue(json, Action.class);

        Action.AppendMessage message = assertInstanceOf(Action.AppendMessage.class, action);
        assertEquals("append-message", message.getAction());
        assertEquals("0d1c8f4e-6b2a-4d55-9c31-7f0e2a5b8d10", message.getTicketId());
        assertEquals("user", message.getRole());
        assertEquals("Не приходит письмо со ссылкой на сброс пароля", message.getText());
    }

    @Test
    void deserializesListTicket() throws Exception {
        String json = """
                {
                  "action": "list-my-tickets",
                  "user_id": "ru743@gmail.com"
                }
                """;

        Action action = mapper.readValue(json, Action.class);

        Action.ListTicket list = assertInstanceOf(Action.ListTicket.class, action);
        assertEquals("list-my-tickets", list.getAction());
        assertEquals("ru743@gmail.com", list.getUserId());
    }
}
