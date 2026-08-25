package ru.hexlet.llm.developer425.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;


public class CreateTicketHandler implements Function<Object, Object> {

    private static final Logger LOG = LoggerFactory.getLogger(CreateTicketHandler.class);

    @Override
    public Object apply(Object o) {
        LOG.info("Got request: {}", o);
        return "SUCCESS";
    }


}
