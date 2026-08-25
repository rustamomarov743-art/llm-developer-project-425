package ru.hexlet.llm.developer425.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yandex.cloud.sdk.functions.Context;
import yandex.cloud.sdk.functions.YcFunction;

public class YdbTicketsHandler implements YcFunction<String, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(YdbTicketsHandler.class);

    @Override
    public Object handle(String input, Context context) {

        return null;
    }
}
