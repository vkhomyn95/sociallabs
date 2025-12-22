package com.workflow.sociallabs.model;

public enum NodeDiscriminator {

    TELEGRAM_BOT_ACTION(Values.TELEGRAM_BOT_ACTION),
    TELEGRAM_BOT_TRIGGER(Values.TELEGRAM_BOT_TRIGGER),
    TELEGRAM_CLIENT_ACTION(Values.TELEGRAM_CLIENT_ACTION),
    TELEGRAM_CLIENT_TRIGGER(Values.TELEGRAM_CLIENT_TRIGGER);

    // (Опціонально) Зберігаємо значення, якщо воно вам треба в логіці
    public final String value;

    NodeDiscriminator(String value) {
        this.value = value;
    }

    // Статичний внутрішній клас для констант часу компіляції
    public static class Values {
        public static final String TELEGRAM_BOT_ACTION = "telegram_bot_action";
        public static final String TELEGRAM_BOT_TRIGGER = "telegram_bot_trigger";
        public static final String TELEGRAM_CLIENT_ACTION = "telegram_client_action";
        public static final String TELEGRAM_CLIENT_TRIGGER = "telegram_client_trigger";
    }
}