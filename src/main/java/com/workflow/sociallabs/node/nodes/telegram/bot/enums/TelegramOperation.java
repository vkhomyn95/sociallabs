package com.workflow.sociallabs.node.nodes.telegram.bot.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Тип операції
 */
@Getter
@RequiredArgsConstructor
public enum TelegramOperation {
    SEND("send"),
    EDIT("edit"),
    DELETE("delete"),
    PIN("pin"),
    UNPIN("unpin"),
    GET("get"),
    FORWARD("forward");

    @JsonValue
    private final String value;

    public static TelegramOperation fromValue(String value) {
        for (TelegramOperation op : values()) {
            if (op.value.equals(value)) {
                return op;
            }
        }
        return SEND; // default
    }
}