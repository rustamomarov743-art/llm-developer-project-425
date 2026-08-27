package ru.hexlet.llm.developer425.ticket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.hexlet.llm.developer425.core.Json;

import java.util.Objects;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Event.CreateTicket.class, name = Event.CREATE_TICKET),
        @JsonSubTypes.Type(value = Event.AppendMessage.class, name = Event.APPEND_MESSAGE),
        @JsonSubTypes.Type(value = Event.ListTicket.class, name = Event.LIST_TICKET)
})
public abstract sealed class Event {
    private static final ObjectMapper mapper = Json.mapper();

    public static final String CREATE_TICKET = "create-ticket";
    public static final String APPEND_MESSAGE = "append-message";
    public static final String LIST_TICKET = "list-my-tickets";

    private final String action;

    private Event(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public abstract void validate() throws Exception;

    public static Event deserialize(String json) throws Exception {
        Objects.requireNonNull(json, "json is null");
        Event obj;
        try {
            obj = mapper.readValue(json, Event.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize from json", e);
        }
        obj.validate();
        return obj;
    }

    public static final class CreateTicket extends Event {

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("category")
        private String category;

        @JsonProperty("text")
        private String text;

        CreateTicket() {
            super(CREATE_TICKET);
        }

        public String getUserId() {
            return userId;
        }

        public String getCategory() {
            return category;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "CreateTicket{" +
                   "action='" + getAction() + '\'' +
                   ", userId='" + userId + '\'' +
                   ", category='" + category + '\'' +
                   ", text='***'" +
                   "} " + super.toString();
        }

        @Override
        public void validate() throws Exception {
            Objects.requireNonNull(getAction(), "action must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(category, "category must not be null");
            Objects.requireNonNull(text, "text must not be null");
        }
    }

    public static final class AppendMessage extends Event {

        @JsonProperty("ticket_id")
        private String ticketId;

        @JsonProperty("role")
        private String role;

        @JsonProperty("text")
        private String text;

        AppendMessage() {
            super(APPEND_MESSAGE);
        }

        public String getTicketId() {
            return ticketId;
        }

        public String getRole() {
            return role;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "AppendMessage{" +
                   "action='" + getAction() + '\'' +
                   ", ticketId='" + ticketId + '\'' +
                   ", role='" + role + '\'' +
                   ", text='***'" +
                   "} " + super.toString();
        }

        @Override
        public void validate() throws Exception {
            Objects.requireNonNull(getAction(), "action must not be null");
            Objects.requireNonNull(ticketId, "ticketId must not be null");
            Objects.requireNonNull(role, "role must not be null");
            Objects.requireNonNull(text, "text must not be null");
        }
    }

    public static final class ListTicket extends Event {

        @JsonProperty("user_id")
        private String userId;

        ListTicket() {
            super(LIST_TICKET);
        }

        public String getUserId() {
            return userId;
        }

        @Override
        public String toString() {
            return "ListTicket{" +
                   "action='" + getAction() + '\'' +
                   ", userId='" + userId + '\'' +
                   "} " + super.toString();
        }

        @Override
        public void validate() throws Exception {
            Objects.requireNonNull(getAction(), "action must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
        }
    }
}
