package ru.hexlet.llm.developer425.ticket.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Проверяет, что Jackson по полю {@code event} выбирает нужного наследника {@link Event}.
 * JSON в тестах повторяет то, что MCP-шлюз кладёт в тело запроса к функции ydb-tickets.
 */
class EventDeserializationTest {

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

        Event event = Event.deserialize(json);

        Event.CreateTicket ticket = assertInstanceOf(Event.CreateTicket.class, event);
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

        Event event = Event.deserialize(json);

        Event.AppendMessage message = assertInstanceOf(Event.AppendMessage.class, event);
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

        Event event = Event.deserialize(json);

        Event.ListTicket list = assertInstanceOf(Event.ListTicket.class, event);
        assertEquals("list-my-tickets", list.getAction());
        assertEquals("ru743@gmail.com", list.getUserId());
    }
}
