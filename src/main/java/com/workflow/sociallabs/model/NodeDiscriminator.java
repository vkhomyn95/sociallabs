package com.workflow.sociallabs.model;

public enum NodeDiscriminator {

    // Communications
    TELEGRAM_BOT_ACTION(Values.TELEGRAM_BOT_ACTION),
    TELEGRAM_BOT_TRIGGER(Values.TELEGRAM_BOT_TRIGGER),
    TELEGRAM_CLIENT_ACTION(Values.TELEGRAM_CLIENT_ACTION),
    TELEGRAM_CLIENT_TRIGGER(Values.TELEGRAM_CLIENT_TRIGGER),

    // Logics
    IF_LOGIC(Values.IF_LOGIC),
    SWITCH_LOGIC(Values.SWITCH_LOGIC),

    // AI Agent
    AI_AGENT(Values.AI_AGENT),
    AI_CHAT(Values.AI_CHAT),
    AI_STRUCTURED_OUTPUT(Values.AI_STRUCTURED_OUTPUT);

    // (Опціонально) Зберігаємо значення, якщо воно вам треба в логіці
    public final String value;

    NodeDiscriminator(String value) {
        this.value = value;
    }

    // Статичний внутрішній клас для констант часу компіляції
    public static class Values {

        // Communications
        public static final String TELEGRAM_BOT_ACTION = "telegram_bot_action";
        public static final String TELEGRAM_BOT_TRIGGER = "telegram_bot_trigger";
        public static final String TELEGRAM_CLIENT_ACTION = "telegram_client_action";
        public static final String TELEGRAM_CLIENT_TRIGGER = "telegram_client_trigger";

        // Logics
        public static final String IF_LOGIC = "if_logic";
        public static final String SWITCH_LOGIC = "switch_logic";

        // AI Agent
        public static final String AI_AGENT = "ai_agent";
        public static final String AI_CHAT = "ai_chat";
        public static final String AI_STRUCTURED_OUTPUT = "ai_structured_output";
    }
}