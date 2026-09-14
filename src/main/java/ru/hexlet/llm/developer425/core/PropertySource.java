package ru.hexlet.llm.developer425.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class PropertySource {
    private static final Logger LOG = LoggerFactory.getLogger(PropertySource.class);

    private PropertySource() {
    }

    public static String getOrDefault(String key, String defaultValue) {
        Objects.requireNonNull(key, key + " must not be null");
        return Optional.ofNullable(System.getenv(key))
                .orElse(defaultValue);
    }

    public static String get(String key) {
        return Optional.ofNullable(System.getenv(key))
                .orElseThrow(() -> new IllegalStateException("Missing env var: " + key));
    }

    public static <T> T getOrDefault(String key, T defaultValue, Function<String, T> converter) {
        return getOptional(key, converter)
                .orElse(defaultValue);
    }

    public static <T> T get(String key, Function<String, T> converter) {
        return getOptional(key, converter)
                .orElseThrow(() -> new IllegalStateException("Missing env var: " + key));
    }

    private static <T> Optional<T> getOptional(String key, Function<String, T> converter) {
        Objects.requireNonNull(key, key + " must not be null");
        return Optional.ofNullable(System.getenv(key))
                .map(s -> {
                    try {
                        return converter.apply(s);
                    } catch (Exception e) {
                        LOG.error("Failed to convert {} from : {} ", key, s);
                        throw new RuntimeException(e);
                    }
                });
    }

}
