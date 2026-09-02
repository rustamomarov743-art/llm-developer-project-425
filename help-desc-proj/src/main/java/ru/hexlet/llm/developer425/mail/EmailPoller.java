package ru.hexlet.llm.developer425.mail;

import jakarta.mail.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.agent.AgentService;
import ru.hexlet.llm.developer425.core.BotStateSource;
import ru.hexlet.llm.developer425.core.Iam;
import ru.hexlet.llm.developer425.core.PropertySource;
import ru.hexlet.llm.developer425.guard.ComplexIntentDetector;
import ru.hexlet.llm.developer425.ticket.TicketService;
import yandex.cloud.sdk.functions.Context;
import yandex.cloud.sdk.functions.YcFunction;

import java.util.Objects;
import java.util.function.Supplier;

public class EmailPoller implements YcFunction<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(EmailPoller.class);

    @Override
    public String handle(String o, Context context) {
        LOG.info("EmailPoller invoked");
        try {
            return doApply(context);
        } catch (Exception e) {
            LOG.error("Failed to call function with message: %s".formatted(e.getMessage()), e);
            return "ERROR";
        }
    }

    private String doApply(Context context) throws Exception {
        String token = Iam.token(context);

        String email = PropertySource.get("HELPDESK_MAILBOX");
        String smtpUser = PropertySource.get("SMTP_USER");
        String smtpHost = PropertySource.get("SMTP_HOST");
        String smtpPort = PropertySource.get("SMTP_PORT");
        String imapHost = PropertySource.get("IMAP_HOST");
        String imapPort = PropertySource.get("IMAP_PORT");
        String password = PropertySource.get("IMAP_PASSWORD");
        int batch = PropertySource.getOrDefault("EMAIL_BATCH", 5, Integer::parseInt);
        String folderId = PropertySource.get("YC_FOLDER_ID");
        String agentId = PropertySource.get("YC_AGENT_ID");
        String mcpServerUrl = PropertySource.get("YC_YDB_TICKETS_MCP_SERVER_URL");
        String vectorStoreId = PropertySource.get("YC_VECTOR_STORE_ID");
        String guardModel = PropertySource.getOrDefault("YC_GUARD_MODEL", "yandexgpt-lite");

        Supplier<String> tokenSupplier = () -> token;
        AgentService agent = new AgentService(tokenSupplier, folderId, agentId, mcpServerUrl, vectorStoreId);
        ComplexIntentDetector intentDetector = new ComplexIntentDetector(tokenSupplier, folderId, guardModel);
        Session session = MailSessionBuilder.build(smtpUser, password, smtpHost, smtpPort, imapHost, imapPort, email);
        TicketService ticketService = new TicketService();
        MailProcessingService mailService = new MailProcessingService(batch, session, agent, ticketService,
                intentDetector);

        String processedMessageNum = BotStateSource.get("last_processed_message_num");
        Integer lastMessageNum = Objects.isNull(processedMessageNum) ? null : Integer.valueOf(processedMessageNum);
        lastMessageNum = mailService.processUnreadMessages(lastMessageNum);
        BotStateSource.save("last_processed_message_num", String.valueOf(lastMessageNum));
        return "SUCCESS";
    }
}
