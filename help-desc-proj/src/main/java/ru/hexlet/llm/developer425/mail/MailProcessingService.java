package ru.hexlet.llm.developer425.mail;

import jakarta.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.agent.AgentService;

import java.util.Arrays;
import java.util.Objects;

public class MailProcessingService {

    private static final Logger LOG = LoggerFactory.getLogger(MailProcessingService.class);

    private final int batch;
    private final Session session;
    private final AgentService agentService;

    public MailProcessingService(int batch, Session session, AgentService agentService) {
        this.agentService = agentService;
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