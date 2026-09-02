package ru.hexlet.llm.developer425.guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hexlet.llm.developer425.core.Text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class RegexpIntentDetector implements IntentDetector {

    private static final Logger LOG = LoggerFactory.getLogger(RegexpIntentDetector.class);

    public static final RegexpIntentDetector INSTANCE = new RegexpIntentDetector();

    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>();

    static {
        // Классика: отмена ранее данных инструкций. Ловится связка «глагол отмены + объект»,
        // а не отдельные слова: «инструкции из письма не помогли» — обычное обращение.
        //
        // Промежуток между глаголом и объектом описан как [^.]{0,40}?, а НЕ [^.\n]{0,40}?:
        // перевод строки разрывать связку не должен. Обнаружено тестом склейки полей —
        // «Прошу игнорировать» в заголовке и «все прежние инструкции» в теле давали между
        // словами \n, и образец переставал совпадать. То же касается атаки, разложенной по
        // строкам внутри одного поля. Точка в стоп-классе остаётся: она отделяет
        // предложения, и без неё «...не помогли. Инструкции...» дало бы ложное срабатывание.
        PATTERNS.put("INJ_IGNORE_PREVIOUS", Pattern.compile(
                "(?iuU)\\b(ignore|disregard|forget|override)\\b[^.]{0,40}?"
                + "\\b(instructions?|prompts?|rules?|context)\\b"
                + "|\\b(игнорируй|игнорировать|забудь|забыть|отмени|отбрось)\\b"
                + "[^.]{0,40}?\\b(инструкц\\w+|указани\\w+|правил\\w+|промпт\\w*)"));

        // Подмена роли: сюда же попадает эмоциональный промптинг «представь, что ты…».
        PATTERNS.put("INJ_ROLE_OVERRIDE", Pattern.compile(
                "(?iuU)\\b(you\\s+are\\s+now|act\\s+as|pretend\\s+to\\s+be|roleplay)\\b"
                + "|\\b(представь,?\\s+что\\s+ты|притворись|веди\\s+себя\\s+как"
                + "|теперь\\s+ты\\s+\\p{IsCyrillic}+)"));

        // Текст, выданный за системное сообщение. Якорь на начало строки — отсюда
        // требование сохранять переводы строк при нормализации.
        PATTERNS.put("INJ_FAKE_SYSTEM", Pattern.compile(
                "(?iuU)(^|\\n)\\s*(#{1,6}\\s*|\\*{0,2})"
                + "(system|system_prompt|assistant|developer|система|ассистент)"
                + "\\s*[:>]|</?\\s*(system|system_prompt)\\s*>|\\[/?inst\\]"));

        // Просьба вынести наружу секрет или собственные инструкции агента.
        PATTERNS.put("INJ_SECRET_EXFIL", Pattern.compile(
                "(?iuU)\\b(github_token|ydb_connection_string|folder_id|aws_secret\\w*)\\b"
                + "|\\b(переменн\\w+\\s+окружения|environment\\s+variable"
                + "|системн\\w+\\s+(промпт|инструкц\\w+)|system\\s+prompt"
                + "|тво[иеё]\\w*\\s+инструкц\\w+|your\\s+instructions)\\b"));

        // Утечка через ссылку: данные уезжают параметром запроса. Ссылка без такого
        // параметра не трогается — она обычная часть баг-репорта.
        PATTERNS.put("INJ_LINK_EXFIL", Pattern.compile(
                "(?iuU)https?://\\S+[?&]\\s*"
                + "(d|q|c|data|context|payload|prompt|token|secret|dump)\\s*="));

        // Попытка управлять не агентом, а проверкой намерения: текст обращается к
        // классификатору, диктует ему вердикт или подделывает маркеры блока данных.
        //
        // Образец появился по итогам замера, а не из предположения: случаи
        // classifier-marker и classifier-appeal классификатор пропускал как SAFE —
        // он верит тексту о самом себе. Регулярка тут уместнее модели: обычный
        // пользователь поддержки не пишет «ответь SAFE» и не упоминает маркеры.
        PATTERNS.put("INJ_CLASSIFIER_TAMPER", Pattern.compile(
                "(?iuU)\\b(respond|reply|answer|ответь|отвечай|верни|вердикт|verdict)\\b"
                + "[^.\\n]{0,30}\\b(safe|attack|безопасн\\w+)\\b"
                + "|\\b(safety\\s+classifier|классификатор\\w*\\s+безопасности)\\b"
                + "|<<<[^>\\n]{0,30}(данных|data)[^>\\n]{0,10}>>>"));

        // Разрушительная команда. Требуется квантор «все»: «удалите мой тикет» — обычная
        // просьба, «удали все тикеты» — нет.
        PATTERNS.put("INJ_DESTRUCTIVE", Pattern.compile(
                "(?iuU)\\b(delete|remove|drop|wipe|purge)\\s+(all|every|the\\s+entire)\\b"
                + "|\\b(удали|удалить|сотри|стереть|очисти|очистить)\\s+(все|всё|весь)\\b"));
    }

    private RegexpIntentDetector() {
    }

    @Override
    public Optional<ContentType> classify(String message) {
        Objects.requireNonNull(message, "message must not be null");
        return find(message)
                .map(pattern -> {
                    LOG.warn("Pattern {} found", pattern);
                    return ContentType.INJECTION;
                });
    }


    /**
     * @param text исходный текст; нормализация выполняется внутри
     * @return имя сработавшего образца или пустое значение
     */
    private Optional<String> find(String text) {
        String normalized = Text.normalize(text);

        for (Map.Entry<String, Pattern> entry : PATTERNS.entrySet()) {
            if (entry.getValue().matcher(normalized).find()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
}
