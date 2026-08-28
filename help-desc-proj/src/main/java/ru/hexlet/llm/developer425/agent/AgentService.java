package ru.hexlet.llm.developer425.agent;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.credential.BearerTokenCredential;
import com.openai.models.responses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.agent.model.AgentResponse;
import ru.hexlet.llm.developer425.agent.model.UserMessage;
import ru.hexlet.llm.developer425.core.Json;
import ru.hexlet.llm.developer425.ticket.model.CreateTicketResponse;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class AgentService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentService.class);

    private final OpenAIClient client;
    private final ResponseCreateParams.Builder paramBuilder;
    private final ResponsePrompt.Builder promptBuilder;
    private final String mcpServerUrl;

    public static void main(String[] args) {
        var agentService =
                new AgentService(() -> "change_me",
                        "b1gpecvq19l0fva2r6mc", "fvtdutb2q552omlr99sq",
                        "https://db8asqgevmh9ih0tb4m2.99igvxy3.mcpgw.serverless.yandexcloud.net");

        var response = agentService
                .sendMessage(new UserMessage("my@example.ru", "создай тикет. У меня ничего не работает принтер"));
        LOG.info(response.toString());
    }

    public AgentService(Supplier<String> tokenSupplier,
                        String folderId,
                        String agentId,
                        String mcpServerUrl) {
        Objects.requireNonNull(tokenSupplier, "tokenSupplier must not be null");
        Objects.requireNonNull(folderId, "folderId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        this.client = OpenAIOkHttpClient.builder()
                .credential(BearerTokenCredential.create(tokenSupplier))
                .baseUrl("https://ai.api.cloud.yandex.net/v1")
                .project(folderId)
                .build();
        this.promptBuilder = ResponsePrompt.builder()
                .id(agentId);
        this.paramBuilder = ResponseCreateParams.builder();
        this.mcpServerUrl = mcpServerUrl;
    }

    public AgentResponse sendMessage(UserMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        ResponsePrompt.Variables variables = ResponsePrompt.Variables.builder()
                .putAdditionalProperty("user_id", JsonValue.from(message.userId()))
                .build();
        ResponseCreateParams params = paramBuilder
                .prompt(promptBuilder
                        .variables(variables)
                        .build())
                .input(message.text())
                .tools(List.of(Tool.ofMcp(Tool.Mcp.builder()
                        .type(JsonValue.from("mcp"))
                        .serverLabel("ydb-tickets-mcp")
                        .serverUrl(mcpServerUrl)
                        .serverDescription("")
                        .requireApproval(Tool.Mcp.RequireApproval.McpToolApprovalSetting.NEVER)
                        .allowedTools(Tool.Mcp.AllowedTools.McpToolFilter.builder()
                                .toolNames(List.of("create-ticket", "list-my-tickets"))
                                .readOnly(false)
                                .build())
                        .build())))
                .build();
        long begin = System.currentTimeMillis();
        var response = client.responses().create(params);
        long end = System.currentTimeMillis();

        return parseResponse(response, end - begin);
    }

    private AgentResponse parseResponse(Response response, long latencyMs) {
        String model = response.model().string().orElse("");
        Long inputTokens = null;
        Long outputTokens = null;
        var usage = response.usage()
                .orElse(null);
        if (Objects.nonNull(usage)) {
            inputTokens = usage.inputTokens();
            outputTokens = usage.outputTokens();
        }
        String createdTicketId = response.output()
                .stream()
                .map(item -> item.mcpCall().orElse(null))
                .filter(Objects::nonNull)
                .filter(mcpCall -> "create-ticket".equalsIgnoreCase(mcpCall.name()))
                .flatMap(mcpCall -> mcpCall.output().stream())
                .map(this::parseTicketId)
                .flatMap(Optional::stream)
                .findAny()
                .orElse(null);

        String text = response.output()
                .stream()
                .map(item -> item.message().orElse(null))
                .filter(Objects::nonNull)
                .filter(message -> ResponseOutputMessage.Status.COMPLETED.equals(message.status()))
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(ResponseOutputText::text)
                .findAny()
                .orElse(null);

        return new AgentResponse(model, createdTicketId, text, inputTokens, outputTokens, latencyMs);
    }

    private Optional<String> parseTicketId(String json) {
        try {
            return Optional.ofNullable(Json.mapper().readValue(json, CreateTicketResponse.class).ticketId());
        } catch (JsonProcessingException e) {
            LOG.error("Error parsing ticket id from json", e);
            return Optional.empty();
        }

    }
}

