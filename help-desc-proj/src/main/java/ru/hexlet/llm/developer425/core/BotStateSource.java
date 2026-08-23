package ru.hexlet.llm.developer425.core;

import tech.ydb.query.tools.QueryReader;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.NullValue;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Value;

import java.util.Objects;

public class BotStateSource {

    private static final String GET_VALUE = """
            DECLARE $key AS Utf8;
            
            SELECT value
            FROM bot_state
            WHERE key = $key;
            """;

    private static final String INSERT_VALUE = """
            DECLARE $key AS Utf8;
            DECLARE $value AS Utf8;
            
            UPSERT INTO bot_state (key, value)
            SELECT $key, $value;
            """;

    private BotStateSource() {
    }

    public static String get(String key) {
        Objects.requireNonNull(key, "key must be set");
        Params params = Params.of("$key", PrimitiveValue.newText(key));
        QueryReader reader = Ydb.read(GET_VALUE, params);
        ResultSetReader resultSet = reader.getResultSet(0);
        if (resultSet.next()) {
            ValueReader value = resultSet.getColumn("value");
            return value.isOptionalItemPresent() ? value.getText() : null;
        }
        return null;
    }

    public static void save(String key, String value) {
        Objects.requireNonNull(key, "key must be set");
        Params params = Params.of("$key", PrimitiveValue.newText(key));
        Value<?> value1 = Objects.isNull(value) ? NullValue.of() : PrimitiveValue.newText(value);
        params.put("$value", value1);
        Ydb.execute(INSERT_VALUE, params);
    }
}
