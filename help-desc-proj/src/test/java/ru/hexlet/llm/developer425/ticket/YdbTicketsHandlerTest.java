package ru.hexlet.llm.developer425.ticket;

import org.junit.jupiter.api.Test;

class YdbTicketsHandlerTest {


    @Test
    void handleCreateTicket() throws Exception {
        String json = """
                {
                     "action": "create-ticket",
                     "category": "bug",
                     "text": "все не работает, не могу понять в чем дело",
                     "user_id": "exaple@mail.ru"
                 }
                """;

        new YdbTicketsHandler().handle(json, null);
    }
}