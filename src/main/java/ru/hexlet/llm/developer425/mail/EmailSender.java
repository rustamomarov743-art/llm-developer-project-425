package ru.hexlet.llm.developer425.mail;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.core.PropertySource;
import ru.hexlet.llm.developer425.mail.model.SendEmailRequest;
import yandex.cloud.sdk.functions.Context;
import yandex.cloud.sdk.functions.YcFunction;

import java.util.Map;

public class EmailSender implements YcFunction<String, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(EmailSender.class);

    @Override
    public Object handle(String input, Context context) {
        LOG.info("EmailSender invoked");
        try {
            doApply(SendEmailRequest.deserialize(input));
            return Map.of("success", true);
        } catch (Exception e) {
            LOG.error("Failed to call function with message: %s".formatted(e.getMessage()), e);
            return Map.of("success", false);
        }
    }

    private void doApply(SendEmailRequest request) throws Exception {
        String operatorEmail = PropertySource.get("OPERATOR_EMAIL");
        String email = PropertySource.get("HELPDESK_MAILBOX");
        String smtpUser = PropertySource.get("SMTP_USER");
        String smtpHost = PropertySource.get("SMTP_HOST");
        String smtpPort = PropertySource.get("SMTP_PORT");
        String password = PropertySource.get("SMTP_PASSWORD");

        Session session = MailSessionBuilder.build(smtpUser, password, smtpHost, smtpPort, email);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(email));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(operatorEmail));
        message.setSubject(request.getSubject(), "UTF-8");
        message.setText(request.getBody(), "UTF-8");
        Transport.send(message);
    }
}
