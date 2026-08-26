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

    /** Статус новой заявки; answered, escalated и closed выставляются позже по ходу переписки. */
    private static final String NEW_TICKET_STATUS = "open";

    /** Метрики модели есть только у ответов агента, у сообщения пользователя их взять негде. */
    private static final String NO_MODEL = "";
    private static final long NO_TOKENS = 0;
    private static final long NO_LATENCY = 0;

    public AppendMessageResponse appendMessage(Event.AppendMessage event, Context context) {
        Objects.requireNonNull(event);
        String messageId = UUID.randomUUID().toString();

        TicketRepository.saveMessage(messageId, event.getTicketId(), event.getRole(),
                event.getText(), NO_MODEL, NO_TOKENS, NO_TOKENS, NO_LATENCY, Instant.now());

        return new AppendMessageResponse(messageId, true);
    }

    public CreateTicketResponse createTicket(Event.CreateTicket event, Context context) {
        Objects.requireNonNull(event);
        String ticketId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();

        TicketRepository.saveTicket(ticketId, event.getUserId(), event.getCategory(),
                NEW_TICKET_STATUS, event.getText(), createdAt);

        return new CreateTicketResponse(ticketId, createdAt);
    }

    public List<TicketResponse> findTickets(Event.ListTicket event, Context context) {
        Objects.requireNonNull(event);
        return TicketRepository.findTickets(event.getUserId());
    }
}
