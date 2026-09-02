package ru.hexlet.llm.developer425.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PiiTest {

    @Test
    @DisplayName("телефон маскируется в формате из задания, последние две цифры остаются")
    void телефонМаскируетсяПоФормату() {
        Pii.Masked masked = Pii.mask("Мой телефон +7 (999) 123-45-67, жду звонка.");

        assertEquals("Мой телефон +7 (***) ***-**-67, жду звонка.", masked.text());
        assertEquals(java.util.List.of("PII_PHONE_RU"), masked.applied());
    }

    @Test
    @DisplayName("телефон опознаётся и через восьмёрку, и без разделителей")
    void телефонВРазныхЗаписях() {
        assertEquals("+7 (***) ***-**-67", Pii.mask("89991234567").text());
        assertEquals("+7 (***) ***-**-67", Pii.mask("+7 999 123 45 67").text());
    }

    @Test
    @DisplayName("почта: остаётся первая буква и домен")
    void почтаМаскируется() {
        Pii.Masked masked = Pii.mask("пишите на rustam.omarov@gmail.com");

        assertEquals("пишите на r***@gmail.com", masked.text());
    }

    @Test
    @DisplayName("карта маскируется до последних четырёх цифр")
    void картаМаскируется() {
        Pii.Masked masked = Pii.mask("платил картой 4276 3800 1234 5679");

        assertEquals("платил картой **** **** **** 5679", masked.text());
        assertEquals(java.util.List.of("PII_CARD"), masked.applied());
    }

    /**
     * Причина существования проверки Луна: без неё маска легла бы на любые шестнадцать
     * цифр, и номер заказа из тикета исчез бы вместе с поводом его завести.
     */
    @Test
    @DisplayName("длинное число, не проходящее проверку Луна, картой не считается")
    void номерЗаказаНеКарта() {
        Pii.Masked masked = Pii.mask("Номер заказа 1234 5678 9012 3456.");

        assertEquals("Номер заказа 1234 5678 9012 3456.", masked.text());
        assertTrue(masked.applied().isEmpty());
        assertFalse(Pii.passesLuhn("1234567890123456"));
        assertTrue(Pii.passesLuhn("4276380012345679"));
    }

    /**
     * Порядок правил: карта, начинающаяся с восьмёрки, частично подходит под образец
     * российского телефона. При обратном порядке от неё остался бы незамаскированный хвост.
     */
    @Test
    @DisplayName("карта с восьмёрки маскируется целиком, а не как телефон")
    void картаМаскируетсяРаньшеТелефона() {
        Pii.Masked masked = Pii.mask("карта 8776 8500 1234 5677");

        assertEquals("карта **** **** **** 5677", masked.text());
        assertEquals(java.util.List.of("PII_CARD"), masked.applied());
    }

    @Test
    @DisplayName("три вида PII в одном обращении маскируются вместе")
    void всеТриВидаСразу() {
        Pii.Masked masked = Pii.mask(
                "телефон +7 (999) 123-45-67, почта a@b.ru, карта 4276 3800 1234 5679");

        assertEquals("телефон +7 (***) ***-**-67, почта a***@b.ru, карта **** **** **** 5679",
                masked.text());
        assertEquals(java.util.List.of("PII_CARD", "PII_PHONE_RU", "PII_EMAIL"),
                masked.applied());
    }

    @Test
    @DisplayName("обычный текст без PII не меняется")
    void текстБезPiiНеМеняется() {
        String text = "Не приходит письмо для сброса пароля, пробовал трижды.";

        assertEquals(text, Pii.mask(text).text());
    }

}