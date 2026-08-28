package ru.hexlet.llm.developer425.mail;

import jakarta.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.agent.AgentService;
import ru.hexlet.llm.developer425.agent.model.AgentResponse;
import ru.hexlet.llm.developer425.agent.model.UserMessage;
import ru.hexlet.llm.developer425.ticket.TicketService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class MailProcessingService {

    private static final Logger LOG = LoggerFactory.getLogger(MailProcessingService.class);

    private final int batch;
    private final Session session;
    private final AgentService agentService;
    private final TicketService ticketService;

    public MailProcessingService(int batch, Session session, AgentService agentService, TicketService ticketService) {
        this.agentService = agentService;
        this.ticketService = ticketService;
        if (batch < 1) {
            throw new IllegalArgumentException("Batch must be grater then 1");
        }
        this.batch = batch;
        this.session = session;
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
            if (!message.isSet(Flags.Flag.SEEN)) {
                LOG.info("GOT_UNSEEN=1");
                try {
                    process(message, transport);
                } catch (MessagingException e) {
                    LOG.error("Failed to process message num: %s".formatted(message.getMessageNumber()), e);
                    throw new RuntimeException(e);
                }
                message.setFlag(Flags.Flag.SEEN, true);
            }
            lastProcessed = message.getMessageNumber();
        }
        return lastProcessed;
    }

    private void process(Message message, Transport transport) throws MessagingException {
        String subject = message.getSubject();
        Address[] from = message.getFrom();
        Optional<Address> optionalAddress = Arrays.stream(from).findFirst();
        if (optionalAddress.isEmpty()) {
            return;
        }
        LOG.info("MSG num={} from={} subject={}", message.getMessageNumber(), Arrays.toString(from), subject);
        Optional<String> firstText = findFirstText(message);
        if (firstText.isEmpty()) {
            LOG.warn("MSG num={} missing from body", message.getMessageNumber());
            return;
        }
        String userMessage = firstText.get();
        AgentResponse response = agentService
                .sendMessage(new UserMessage(optionalAddress.get().toString(), userMessage));
        if (Objects.isNull(response.text())) {
            LOG.warn("MSG num={} missing response from agent", message.getMessageNumber());
            return;
        }
        sendReply(message, response.text(), transport);
        if (Objects.nonNull(response.createdTicketId())) {
            ticketService.appendMessage(response.createdTicketId(), "user", userMessage, "", 0, 0, 0);
            ticketService.appendMessage(response.createdTicketId(), "agent", response.text(), response.model(),
                    response.inputTokens(), response.outputTokens(), response.latencyMs());
        }
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

    private Optional<String> findFirstText(Message message) throws MessagingException {
        return findFirstTextPart(message);
    }

    private Optional<String> findFirstTextPart(Part part)
            throws MessagingException {

        String disposition = part.getDisposition();

        // Не обрабатываем вложения
        if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
            return Optional.empty();
        }

        // Обычный текст
        if (part.isMimeType("text/plain")) {
            try {
                Object content = part.getContent();

                if (content instanceof String text && !text.isBlank()) {
                    return Optional.of(text);
                }
            } catch (IOException e) {
                throw new MessagingException(
                        "Failed to read text/plain content", e);
            }

            return Optional.empty();
        }

        // HTML
        if (part.isMimeType("text/html")) {
            try {
                Object content = part.getContent();

                if (content instanceof String text && !text.isBlank()) {
                    return Optional.of(text);
                }
            } catch (IOException e) {
                throw new MessagingException(
                        "Failed to read text/html content", e);
            }

            return Optional.empty();
        }

        // multipart/*
        if (part.isMimeType("multipart/*")) {

            try {
                Multipart multipart = (Multipart) part.getContent();

                for (int i = 0; i < multipart.getCount(); i++) {

                    Part child = multipart.getBodyPart(i);

                    Optional<String> result =
                            findFirstTextPart(child);

                    if (result.isPresent()) {
                        return result;
                    }
                }

            } catch (IOException e) {
                throw new MessagingException(
                        "Failed to read multipart content", e);
            }
        }

        return Optional.empty();
    }


}