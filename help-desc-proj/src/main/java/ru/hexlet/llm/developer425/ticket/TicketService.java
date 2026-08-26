package ru.hexlet.llm.developer425.ticket;

import ru.hexlet.llm.developer425.ticket.model.AppendMessageResponse;
import ru.hexlet.llm.developer425.ticket.model.CreateTicketResponse;
import ru.hexlet.llm.developer425.ticket.model.Event;
import ru.hexlet.llm.developer425.ticket.model.TicketResponse;
import yandex.cloud.sdk.functions.Context;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TicketService {

    public AppendMessageResponse appendMessage(Event.AppendMessage event, Context context) {
        Objects.requireNonNull(event);
        return new AppendMessageResponse(UUID.randomUUID().toString(), true);
    }

    public CreateTicketResponse createTicket(Event.CreateTicket event, Context context) {
        Objects.requireNonNull(event);
        return new CreateTicketResponse(UUID.randomUUID().toString(), Instant.now());
    }

    public List<TicketResponse> createTicket(Event.ListTicket event, Context context) {
        Objects.requireNonNull(event);
        return List.of(
                new TicketResponse(UUID.randomUUID().toString(), "open", "bug", "bla-bla-1", Instant.now()),
                new TicketResponse(UUID.randomUUID().toString(), "answered", "feature", "bla-bla-2", Instant.now())
        );
    }
}
