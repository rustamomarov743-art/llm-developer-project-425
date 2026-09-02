package ru.hexlet.llm.developer425.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Маскирование персональных данных до записи и до любого логирования.
 *
 * <p>Формат масок задан критерием задания: {@code +7 (***) ***-**-12} — последние две
 * цифры остаются, по ним человек узнаёт свой номер, а посторонний не восстановит его.
 * Тот же принцип применён к карте (последние четыре) и к почте (первая буква и домен).
 *
 * <p>Порядок важен и проверен тестом: <b>карта маскируется раньше телефона</b>. Номер
 * карты, начинающийся с восьмёрки, частично подходит под образец российского телефона, и
 * при обратном порядке от карты остался бы хвост незамаскированных цифр.
 *
 * <p>Карта дополнительно проверяется алгоритмом Луна. Без этой проверки маска легла бы на
 * любую последовательность из шестнадцати цифр — например, на номер заказа, — и в тикете
 * пропала бы информация, ради которой его завели.
 *
 * <p>Класс не решает, пропускать ли текст: маскирование — не фильтр. Решение принимает
 * {@link Guard}.
 */
public final class Pii {

    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){12,18}\\d\\b");

    private static final Pattern PHONE_RU = Pattern.compile(
            "(?:\\+7|8)[ -]?\\(?(\\d{3})\\)?[ -]?(\\d{3})[ -]?(\\d{2})[ -]?(\\d{2})\\b");

    private static final Pattern EMAIL = Pattern.compile(
            "\\b([\\w.+-]+)@([\\w-]+(?:\\.[\\w-]+)+)\\b");

    private static final int CARD_MIN_DIGITS = 13;
    private static final int CARD_MAX_DIGITS = 19;
    private static final int CARD_VISIBLE_DIGITS = 4;
    private static final int PHONE_VISIBLE_DIGITS = 2;
    private static final int DECIMAL_BASE = 10;

    private Pii() {
    }

    /**
     * Результат маскирования.
     *
     * @param text    текст, в котором PII заменены масками
     * @param applied имена сработавших правил — уезжают в лог вместо самих значений
     */
    public record Masked(String text, List<String> applied) {
    }

    public static Masked mask(String text) {
        List<String> applied = new ArrayList<>();

        String result = maskCards(text, applied);
        result = maskPhones(result, applied);
        result = maskEmails(result, applied);

        return new Masked(result, List.copyOf(applied));
    }

    private static String maskCards(String text, List<String> applied) {
        Matcher matcher = CARD.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String digits = matcher.group().replaceAll("\\D", "");
            if (digits.length() < CARD_MIN_DIGITS || digits.length() > CARD_MAX_DIGITS
                || !passesLuhn(digits)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            String tail = digits.substring(digits.length() - CARD_VISIBLE_DIGITS);
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement("**** **** **** " + tail));
            addOnce(applied, "PII_CARD");
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String maskPhones(String text, List<String> applied) {
        Matcher matcher = PHONE_RU.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String tail = matcher.group(4);
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement("+7 (***) ***-**-" + tail));
            addOnce(applied, "PII_PHONE_RU");
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String maskEmails(String text, List<String> applied) {
        Matcher matcher = EMAIL.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String local = matcher.group(1);
            String domain = matcher.group(2);
            String head = local.isEmpty() ? "" : local.substring(0, 1);
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(head + "***@" + domain));
            addOnce(applied, "PII_EMAIL");
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Алгоритм Луна: контрольная сумма, которой удовлетворяют номера банковских карт.
     * Отличает карту от произвольной длинной цифры вроде номера заказа.
     */
    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubled = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubled) {
                digit *= 2;
                if (digit > DECIMAL_BASE - 1) {
                    digit -= DECIMAL_BASE - 1;
                }
            }
            sum += digit;
            doubled = !doubled;
        }

        return sum % DECIMAL_BASE == 0;
    }

    private static void addOnce(List<String> applied, String rule) {
        if (!applied.contains(rule)) {
            applied.add(rule);
        }
    }
}
