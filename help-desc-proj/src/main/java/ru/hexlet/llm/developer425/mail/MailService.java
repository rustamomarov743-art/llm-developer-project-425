package ru.hexlet.llm.developer425.mail;

import jakarta.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);


    private static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static final String MAIL_SMTP_HOST = "mail.smtp.host";
    private static final String MAIL_SMTP_PORT = "mail.smtp.port";
    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    private static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    private static final String MAIL_IMAP_HOST = "mail.imap.host";
    private static final String MAIL_IMAP_PORT = "mail.imap.port";
    private static final String MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable";


    private final int batch;
    private final Session session;

    public MailService(int batch, String password, String email,
                       String smtpHost, String smtpPort, String imapHost, String imapPort) {
        if (batch < 1) {
            throw new IllegalArgumentException("Batch must be grater then 1");
        }
        this.batch = batch;
        Objects.requireNonNull(password, "password must be set");
        Objects.requireNonNull(email, "email must be set");
        Objects.requireNonNull(smtpHost, "smtpHost must be set");
        Objects.requireNonNull(smtpPort, "smtpPort must be set");
        Objects.requireNonNull(imapHost, "imapHost must be set");
        Objects.requireNonNull(imapPort, "imapPort must be set");
        Properties properties = new Properties();

        properties.put(MAIL_TRANSPORT_PROTOCOL, "smtp");
        properties.put(MAIL_SMTP_HOST, smtpHost);
        properties.put(MAIL_SMTP_PORT, smtpPort);
        properties.put(MAIL_SMTP_AUTH, "true");
        properties.put(MAIL_SMTP_STARTTLS_ENABLE, "true");

        properties.put(MAIL_STORE_PROTOCOL, "imap");
        properties.put(MAIL_IMAP_HOST, imapHost);
        properties.put(MAIL_IMAP_PORT, imapPort);
        properties.put(MAIL_IMAP_SSL_ENABLE, "true");


        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        };
        this.session = Session.getInstance(properties, authenticator);
    }

    public static void main(String[] args) {
        MailService service = new MailService(2,
                "...",
                "...", "smtp.gmail.com", "587", "imap.gmail.com", "993");
        try {
            service.processUnreadMessages(null);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public int processUnreadMessages(Integer lastProcessed) throws MessagingException {
        lastProcessed = Objects.requireNonNullElse(lastProcessed, 0);

        try (Store store = session.getStore(); Transport transport = session.getTransport()) {
            store.connect();


            Folder folder = store.getFolder("INBOX");

            try {
                folder.open(Folder.READ_WRITE);
                int messageCount = folder.getMessageCount();
                if (lastProcessed == messageCount) {
                    LOG.info("No new messages");
                    return lastProcessed;
                } else if (lastProcessed > messageCount) {
                    LOG.warn("Corrupted last processed value. Resetting to 0");
                    lastProcessed = 0;
                }
                int start = lastProcessed + 1;
                int end = Math.min(start + batch - 1, messageCount);

                int messageNumber = process(folder, start, end, transport);
                return messageNumber > 0 ? messageNumber : lastProcessed;
            } finally {
                folder.close(false);
            }
        }
    }

    private int process(Folder folder, int start, int end, Transport transport) throws MessagingException {
        int lastProcessed = -1;
        for (Message message : folder.getMessages(start, end)) {
            process(message, transport);
            lastProcessed = message.getMessageNumber();
        }
        return lastProcessed;
    }

    private void process(Message message, Transport transport) throws MessagingException {
        if (message.isSet(Flags.Flag.SEEN)) {
            return;
        }
        LOG.info("GOT_UNSEEN=1");
        String subject = message.getSubject();
        Address[] from = message.getFrom();
        LOG.info("MSG num={} from={} subject={}", message.getMessageNumber(), Arrays.toString(from), subject);
        sendReply(message, "empty", transport);
        message.setFlag(Flags.Flag.SEEN, true);
    }

    private void sendReply(Message original, String text, Transport transport) throws MessagingException {
        Message reply = original.reply(false);

        String subject = original.getSubject();

        if (subject == null) {
            subject = "";
        }

        if (!subject.toLowerCase().startsWith("re:")) {
            subject = "Re: " + subject;
        }

        reply.setSubject(subject);
        reply.setText(text);

        if (!transport.isConnected()) {
            transport.connect();
        }
        transport.sendMessage(reply, original.getFrom());
    }


}