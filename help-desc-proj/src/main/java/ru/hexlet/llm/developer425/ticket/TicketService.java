package ru.hexlet.llm.developer425.ticket;

import ru.hexlet.llm.developer425.ticket.model.AppendMessageResponse;
import ru.hexlet.llm.developer425.ticket.model.CreateTicketResponse;
import ru.hexlet.llm.developer425.ticket.model.Event;
import ru.hexlet.llm.developer425.ticket.model.TicketResponse;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TicketService {

    private static final String NEW_TICKET_STATUS = "open";

    private static final String NO_MODEL = "";
    private static final long NO_TOKENS = 0;
    private static final long NO_LATENCY = 0;

    public AppendMessageResponse appendMessage(String ticketId, String role, String text,
                                               String model, long tokensIn, long tokensOut, long latencyMs) {
        Objects.requireNonNull(ticketId);
        Objects.requireNonNull(role);
        Objects.requireNonNull(text);
        Objects.requireNonNull(model);
        String messageId = UUID.randomUUID().toString();

        TicketRepository.saveMessage(messageId, ticketId, role, text, model, tokensIn, tokensOut, latencyMs,
                Instant.now());

        return new AppendMessageResponse(messageId, true);
    }

    public AppendMessageResponse appendMessage(Event.AppendMessage event) {
        return appendMessage(event.getTicketId(), event.getRole(), event.getText(), NO_MODEL, NO_TOKENS, NO_TOKENS,
                NO_LATENCY);
    }

    public CreateTicketResponse createTicket(Event.CreateTicket event) {
        Objects.requireNonNull(event);
        String ticketId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();

        TicketRepository.saveTicket(ticketId, event.getUserId(), event.getCategory(),
                NEW_TICKET_STATUS, event.getText(), createdAt);

        return new CreateTicketResponse(ticketId, createdAt);
    }

    public List<TicketResponse> findTickets(Event.ListTicket event) {
        Objects.requireNonNull(event);
        return TicketRepository.findTickets(event.getUserId());
    }
}
