package ru.hexlet.llm.developer425.tg.poller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.core.PropertySource;
import ru.hexlet.llm.developer425.core.Ydb;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.Type;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Function;

public class TelegramPoller implements Function<Object, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramPoller.class);

    private static final String UPDATES_OFFSET_URL = "https://api.telegram.org/bot%s/getUpdates?offset=%s&limit=%s";
    private static final String QUERY = """
            SELECT value
            FROM bot_state
            WHERE key = 'last_telegram_update_id';
            """;

    public static void main(String[] args) {
        new TelegramPoller().getUpdates("...:...", 0, 5);
    }

    @Override
    public Object apply(Object request) {
        LOG.info("TelegramPoller invoked: {}", request);
        try {
            return doApply();
        } catch (Exception e) {
            LOG.error("Failed to call function", e);
            return "ERROR";
        }
    }

    private String doApply() {
        String botToken = PropertySource.get("TELEGRAM_BOT_TOKEN");
        int messageLimit = PropertySource.getOrDefault("TELEGRAM_MESSAGE_LIMIT", 5, Integer::parseInt);
        QueryReader reader = Ydb.read(QUERY);
        ResultSetReader resultSet = reader.getResultSet(0);
        long updateId = -1;
        if (resultSet.next()) {
            ValueReader value = resultSet.getColumn("value");
            updateId = value.isOptionalItemPresent() ? value.getInt64() : -1;
        }
        return getUpdates(botToken, updateId + 1, messageLimit);
    }

    private String getUpdates(String token, long offset, int limit) {
        String url = UPDATES_OFFSET_URL.formatted(token, offset, limit);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));

        try (HttpClient client = builder.build()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOG.info(response.body());
                return "SUCCESS";
            } else {
                LOG.error("API error: HTTP {} -> {}", response.statusCode(), response.body());
                return "ERROR";
            }
        } catch (Exception e) {
            LOG.error("Failed to get updates", e);
            return "ERROR";
        }
    }

    private static ValueReader value(ResultSetReader result, String column) {
        ValueReader reader = result.getColumn(column);
        if (reader.getType().getKind() != Type.Kind.OPTIONAL) {
            return reader;
        }
        if (!reader.isOptionalItemPresent()) {
            throw new IllegalStateException("В колонке " + column + " пусто");
        }
        return reader.getOptionalItem();
    }
}
