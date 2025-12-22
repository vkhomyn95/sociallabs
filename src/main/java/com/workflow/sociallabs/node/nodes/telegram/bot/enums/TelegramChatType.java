package com.workflow.sociallabs.node.nodes.telegram.bot.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Тип чату
 */
@Getter
@RequiredArgsConstructor
public enum TelegramChatType {
    PRIVATE("private"),
    GROUP("group"),
    SUPERGROUP("supergroup"),
    CHANNEL("channel");

    @JsonValue
    private final String value;
}

