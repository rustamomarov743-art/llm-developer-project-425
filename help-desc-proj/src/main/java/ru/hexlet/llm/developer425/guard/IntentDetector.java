package ru.hexlet.llm.developer425.guard;

import java.util.Optional;

public interface IntentDetector {

    Optional<ContentType> classify(String message);
}
