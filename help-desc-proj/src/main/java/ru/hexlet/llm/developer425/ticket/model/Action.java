package ru.hexlet.llm.developer425.ticket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Action.CreateTicket.class, name = Action.CREATE_TICKET ),
        @JsonSubTypes.Type(value = Action.AppendMessage.class, name = Action.APPEND_MESSAGE),
        @JsonSubTypes.Type(value = Action.ListTicket.class, name = Action.LIST_TICKET)
})
public abstract sealed class Action {
    static final String CREATE_TICKET = "create-ticket";
    static final String APPEND_MESSAGE = "append-message";
    static final String LIST_TICKET = "list-my-tickets";

    private final String action;

    private Action(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public abstract void validate() throws Exception;

    public static final class CreateTicket extends Action {

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("category")
        private String category;

        @JsonProperty("text")
        private String text;

        public CreateTicket() {
            super(CREATE_TICKET);
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
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

    public static final class AppendMessage extends Action {

        @JsonProperty("ticket_id")
        private String ticketId;

        @JsonProperty("role")
        private String role;

        @JsonProperty("text")
        private String text;

        public AppendMessage() {
            super(APPEND_MESSAGE);
        }

        public String getTicketId() {
            return ticketId;
        }

        public void setTicketId(String ticketId) {
            this.ticketId = ticketId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
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

    public static final class ListTicket extends Action {

        @JsonProperty("user_id")
        private String userId;

        public ListTicket() {
            super(LIST_TICKET);
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
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
