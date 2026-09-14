package ru.hexlet.llm.developer425.core;

import com.fasterxml.jackson.databind.JsonNode;
import yandex.cloud.sdk.functions.Context;

import java.util.ArrayList;
import java.util.List;

public final class Iam {

    /*
        tokenJson={"access_token":"...","expires_in":43106,"token_type":"Bearer"}
    */
    private static final List<String> TOKEN_FIELDS =
            List.of("access_token", "accessToken", "iamToken", "token");

    private Iam() {
    }

    public static String token(Context context) {
        String json = context.getTokenJson();
        if (json == null || json.isBlank()) {
            throw new IllegalStateException(
                    "Context.getTokenJson() пуст: у версии функции не задан сервисный аккаунт?");
        }

        JsonNode node = Json.parse(json);
        for (String field : TOKEN_FIELDS) {
            String value = node.path(field).asText("");
            if (!value.isBlank()) {
                return value;
            }
        }

        // В сообщение идут только имена полей: значение — сам токен, ему не место в логах.
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        throw new IllegalStateException("В getTokenJson() нет поля с токеном, есть только: " + keys);
    }
}
