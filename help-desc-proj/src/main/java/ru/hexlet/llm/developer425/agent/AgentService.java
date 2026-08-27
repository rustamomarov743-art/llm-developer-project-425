package ru.hexlet.llm.developer425.agent;


import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.credential.BearerTokenCredential;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponsePrompt;
import ru.hexlet.llm.developer425.agent.model.DiscussionLog;
import ru.hexlet.llm.developer425.agent.model.Message;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class AgentService {

    private final OpenAIClient client;
    private final ResponseCreateParams.Builder paramBuilder;

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
                .organization(folderId)
                .build();
        this.paramBuilder = ResponseCreateParams.builder()
                .prompt(ResponsePrompt.builder()
                        .id(agentId)
                        .build())
                .putAdditionalBodyProperty("tools", JsonValue.from(List.of(
                        Map.of(
                                "type", "mcp",
                                "server_label", "ydb-tickets-mcp",
                                "server_url", mcpServerUrl,
                                "server_description", "",
                                "require_approval", "never",
                                "allowed_tools", Map.of(
                                        "tool_names", List.of("create-ticket", "list-my-tickets", "append-message"),
                                        "read_only", false
                                )
                        )
                )));
    }

    public DiscussionLog sendMessage(Message message) {
        Objects.requireNonNull(message, "message must not be null");

        ResponseCreateParams params = paramBuilder
                .input("some message")
                .build();

        var response = client.responses().create(params);

        System.out.println(response.output());
        return null;
    }
}

