package ru.hexlet.llm.developer425.agent.model;

public record AgentResponse(String model, String createdTicketId, String text, Long inputTokens, Long outputTokens,
                            long latencyMs) {

}
