package ru.hexlet.llm.developer425.mail.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.hexlet.llm.developer425.core.Json;

import java.util.Objects;


public class SendEmailRequest {
    private static final ObjectMapper mapper = Json.mapper();


    @JsonProperty("subject")
    private String subject;

    @JsonProperty("body")
    private String body;

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public void validate() throws Exception {
        Objects.requireNonNull(subject, "subject must be set");
        Objects.requireNonNull(body, "body must be set");
    }

    public static SendEmailRequest deserialize(String json) throws Exception {
        Objects.requireNonNull(json, "json is null");
        SendEmailRequest obj;
        try {
            obj = mapper.readValue(json, SendEmailRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize from json", e);
        }
        obj.validate();
        return obj;
    }

}
