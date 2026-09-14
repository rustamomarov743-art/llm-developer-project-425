package ru.hexlet.llm.developer425.core;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Нормализация текста перед сопоставлением с образцами.
 *
 * <p>Приёмы обхода регулярок дешевле самих регулярок, поэтому текст сначала приводится к
 * одному виду, и только потом ищутся образцы. Гасятся три приёма из {@code lesson.md} и
 * практики:
 *
 * <ul>
 *   <li><b>капс</b> — регистр снимается. Капс сам по себе не признак атаки: раздражённый
 *       пользователь пишет так же. Блокировать за регистр — значит мешать работе, а вот
 *       пропустить из-за регистра нельзя;</li>
 *   <li><b>невидимые символы</b> внутри ключевых слов ({@code ig​nore}) — удаляются;</li>
 *   <li><b>совместимые начертания</b> (полноширинные и стилизованные буквы) — сводятся к
 *       обычным нормализацией NFKC.</li>
 * </ul>
 *
 * <p>Переводы строк намеренно сохраняются: по ним опознаётся текст, выданный за системное
 * сообщение ({@code ### SYSTEM:} в начале строки). Схлопываются только пробелы внутри строк.
 *
 * <p>Нормализованный текст используется <b>только для поиска</b>. Хранится и показывается
 * исходный: нормализация ломает читаемость, а база должна содержать то, что написал человек.
 */
public final class Text {

    private static final Pattern INVISIBLE =
            Pattern.compile("[\\u00AD\\u200B-\\u200F\\u2060\\u2062-\\u2064\\uFEFF]");

    /**
     * Только горизонтальные пробелы: переводы строк нужны образцам с якорем на начало строки.
     */
    private static final Pattern HORIZONTAL_SPACES = Pattern.compile("[ \\t\\u00A0\\u2000-\\u200A]+");

    /**
     * Письменности, ожидаемые в канале поддержки. Всё остальное — повод присмотреться.
     */
    private static final Set<Character.UnicodeScript> EXPECTED = Set.of(
            Character.UnicodeScript.CYRILLIC,
            Character.UnicodeScript.LATIN,
            Character.UnicodeScript.COMMON,
            Character.UnicodeScript.INHERITED);

    private Text() {
    }

    public static String normalize(String text) {
        String result = Normalizer.normalize(text, Normalizer.Form.NFKC);
        result = INVISIBLE.matcher(result).replaceAll("");
        result = HORIZONTAL_SPACES.matcher(result).replaceAll(" ");
        return result.toLowerCase(Locale.ROOT);
    }

    /**
     * Письменности, которых в канале поддержки быть не должно (хангыль, иероглифы, арабица).
     *
     * <p>Это <b>метка, а не приговор</b>. Урок прямо предупреждает: блокировать за
     * непривычный источник — крайняя мера, за ним может стоять обычный человек. Поэтому
     * результат уходит в вердикт и в лог как признак, по которому потом видно, чем атака
     * отличалась от обычного обращения, и почему её не взяли регулярки.
     *
     * @return имена письменностей в порядке появления; пустое множество — всё ожидаемо
     */
    public static Set<String> unexpectedScripts(String text) {
        Set<String> found = new LinkedHashSet<>();
        text.codePoints().forEach(codePoint -> {
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (!EXPECTED.contains(script)) {
                found.add(script.name().toLowerCase(Locale.ROOT));
            }
        });
        return found;
    }
}
