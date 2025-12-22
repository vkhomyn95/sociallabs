package com.workflow.sociallabs.node.nodes.telegram.bot.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Parse mode для форматування тексту
 */
@Getter
@RequiredArgsConstructor
public enum TelegramParseMode {
    NONE("none"),
    MARKDOWN("Markdown"),
    MARKDOWN_V2("MarkdownV2"),
    HTML("HTML");

    @JsonValue
    private final String value;

    public static TelegramParseMode fromValue(String value) {
        if (value == null || value.equals("none")) {
            return NONE;
        }
        for (TelegramParseMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return NONE;
    }

    public String getTelegramValue() {
        return this == NONE ? null : value;
    }
}
