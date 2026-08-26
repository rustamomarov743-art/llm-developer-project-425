package ru.hexlet.llm.developer425.ticket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record AppendMessageResponse(@JsonProperty("message_id") String messageId,
                                    @JsonProperty("ok") boolean ok) {

    public AppendMessageResponse {
        Objects.requireNonNull(messageId);
    }
}
