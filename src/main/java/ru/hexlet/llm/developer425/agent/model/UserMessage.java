package ru.hexlet.llm.developer425.agent.model;

import java.util.Objects;

public record UserMessage(String userId, String text) {

    public UserMessage {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(text, "userId must not be null");
    }
}
