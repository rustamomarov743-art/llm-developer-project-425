package ru.hexlet.llm.developer425.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.core.Json;
import ru.hexlet.llm.developer425.ticket.model.Event;
import yandex.cloud.sdk.functions.Context;
import yandex.cloud.sdk.functions.YcFunction;

public class YdbTicketsHandler implements YcFunction<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(YdbTicketsHandler.class);

    private final TicketService ticketService = new TicketService();

    @Override
    public String handle(String input, Context context) {
        Event event;
        try {
            event = Event.deserialize(input);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        LOG.info("Got event={}", event.getAction());
        Object response;
        response = switch (event.getAction()) {
            case Event.CREATE_TICKET -> ticketService.createTicket(((Event.CreateTicket) event));
            case Event.APPEND_MESSAGE -> ticketService.appendMessage(((Event.AppendMessage) event));
            case Event.LIST_TICKET -> ticketService.findTickets(((Event.ListTicket) event));
            default -> throw new IllegalArgumentException("Unknown action: " + event.getAction());
        };
        String responseValue = Json.write(response);
        LOG.debug("response={}", responseValue);
        return responseValue;
    }
}
