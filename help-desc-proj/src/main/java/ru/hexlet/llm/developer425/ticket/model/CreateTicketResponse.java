package ru.hexlet.llm.developer425.ticket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public record CreateTicketResponse(@JsonProperty("ticket_id") String ticketId,
                                   @JsonProperty("created_at") Instant createdAt) {

    public CreateTicketResponse {
        Objects.requireNonNull(ticketId);
        Objects.requireNonNull(createdAt);
    }

}
