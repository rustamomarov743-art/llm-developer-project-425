package ru.hexlet.llm.developer425.agent;


import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.credential.BearerTokenCredential;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponsePrompt;
import com.openai.models.responses.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.agent.model.DiscussionLog;
import ru.hexlet.llm.developer425.agent.model.UserMessage;
import ru.hexlet.llm.developer425.core.Json;

import java.util.List;
import java.util.Objects;
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

        agentService.sendMessage(new UserMessage("my@example.ru", "создай тикет. У меня ничего не работает принтер"));
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

    public DiscussionLog sendMessage(UserMessage message) {
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

        var response = client.responses().create(params);

        LOG.info("Answer from agent: {}", Json.write(response));
        return null;
    }
}

