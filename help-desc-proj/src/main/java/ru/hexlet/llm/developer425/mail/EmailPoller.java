package ru.hexlet.llm.developer425.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.core.BotStateSource;
import ru.hexlet.llm.developer425.core.PropertySource;

import java.util.Objects;
import java.util.function.Function;

public class EmailPoller implements Function<Object, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(EmailPoller.class);

    @Override
    public Object apply(Object o) {
        LOG.info("EmailPoller invoked");
        try {
            return doApply();
        } catch (Exception e) {
            LOG.error("Failed to call function", e);
            return "ERROR";
        }
    }

    private String doApply() throws Exception {
        String email = PropertySource.get("HELPDESK_MAILBOX");
        String smtpHost = PropertySource.get("SMTP_HOST");
        String smtpPort = PropertySource.get("SMTP_PORT");
        String imapHost = PropertySource.get("IMAP_HOST");
        String imapPort = PropertySource.get("IMAP_PORT");
        String password = PropertySource.get("IMAP_PASSWORD");
        int batch = PropertySource.getOrDefault("EMAIL_BATCH", 5, Integer::parseInt);

        MailService mailService = new MailService(batch, password, email, smtpHost, smtpPort, imapHost, imapPort);

        String processedMessageNum = BotStateSource.get("last_processed_message_num");
        Integer lastMessageNum = Objects.isNull(processedMessageNum) ? null : Integer.valueOf(processedMessageNum);
        lastMessageNum = mailService.processUnreadMessages(lastMessageNum);
        BotStateSource.save("last_processed_message_num", String.valueOf(lastMessageNum));
        return "SUCCESS";
    }
}
