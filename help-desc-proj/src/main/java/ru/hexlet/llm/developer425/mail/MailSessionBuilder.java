package ru.hexlet.llm.developer425.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import java.util.Objects;
import java.util.Properties;

public final class MailSessionBuilder {

    private static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static final String MAIL_SMTP_HOST = "mail.smtp.host";
    private static final String MAIL_SMTP_PORT = "mail.smtp.port";
    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    private static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    private static final String MAIL_IMAP_HOST = "mail.imap.host";
    private static final String MAIL_IMAP_PORT = "mail.imap.port";
    private static final String MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable";
    private static final String MAIL_SMTP_FROM = "mail.smtp.from";

    public static Session build(String username, String password,
                                String smtpHost, String smtpPort, String imapHost, String imapPort, String fromEmail) {
        Objects.requireNonNull(password, "password must be set");
        Objects.requireNonNull(username, "username must be set");
        Objects.requireNonNull(smtpHost, "smtpHost must be set");
        Objects.requireNonNull(smtpPort, "smtpPort must be set");
        Objects.requireNonNull(imapHost, "imapHost must be set");
        Objects.requireNonNull(imapPort, "imapPort must be set");
        Objects.requireNonNull(fromEmail, "fromEmail must be set");
        Properties properties = new Properties();

        properties.put(MAIL_TRANSPORT_PROTOCOL, "smtp");
        properties.put(MAIL_SMTP_HOST, smtpHost);
        properties.put(MAIL_SMTP_PORT, smtpPort);
        properties.put(MAIL_SMTP_FROM, fromEmail);
        properties.put(MAIL_SMTP_AUTH, "true");
        properties.put(MAIL_SMTP_STARTTLS_ENABLE, "true");

        properties.put(MAIL_STORE_PROTOCOL, "imap");
        properties.put(MAIL_IMAP_HOST, imapHost);
        properties.put(MAIL_IMAP_PORT, imapPort);
        properties.put(MAIL_IMAP_SSL_ENABLE, "true");

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
        return Session.getInstance(properties, authenticator);
    }
}
