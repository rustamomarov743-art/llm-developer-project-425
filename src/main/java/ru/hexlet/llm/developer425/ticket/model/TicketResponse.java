package ru.hexlet.llm.developer425.ticket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record TicketResponse(@JsonProperty("id") String id,
                             @JsonProperty("status") String status,
                             @JsonProperty("category") String category,
                             @JsonProperty("text") String text,
                             @JsonProperty("created_at") Instant createdAt) {

}
