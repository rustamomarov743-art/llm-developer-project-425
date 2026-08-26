package ru.hexlet.llm.developer425.ticket;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.ticket.model.Event;
import yandex.cloud.sdk.functions.Context;
import yandex.cloud.sdk.functions.YcFunction;

public class YdbTicketsHandler implements YcFunction<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(YdbTicketsHandler.class);

    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .build();

    private final TicketService ticketService = new TicketService();

    @Override
    public String handle(String input, Context context) {
        LOG.debug("input={}", input);
        Event event;
        try {
            event = Event.deserialize(input);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        Object response;
        response = switch (event.getAction()) {
            case Event.CREATE_TICKET -> ticketService.createTicket(((Event.CreateTicket) event), context);
            case Event.APPEND_MESSAGE -> ticketService.appendMessage(((Event.AppendMessage) event), context);
            case Event.LIST_TICKET -> ticketService.findTickets(((Event.ListTicket) event), context);
            default -> throw new IllegalArgumentException("Unknown action: " + event.getAction());
        };
        String responseValue;
        try {
            responseValue = MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize response", e);
        }
        LOG.debug("response={}", responseValue);
        return responseValue;
    }
}
