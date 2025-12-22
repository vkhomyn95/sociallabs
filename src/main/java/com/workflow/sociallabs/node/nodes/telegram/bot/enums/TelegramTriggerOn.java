package com.workflow.sociallabs.node.nodes.telegram.bot.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Тип тригера
 */
@Getter
@RequiredArgsConstructor
public enum TelegramTriggerOn {
    MESSAGE("message"),
    COMMAND("command"),
    CALLBACK_QUERY("callback"),
    INLINE_QUERY("inline"),
    CHANNEL_POST("channelPost"),
    EDITED_MESSAGE("editedMessage"),
    MY_CHAT_MEMBER("myChatMember"),
    CHAT_MEMBER("chatMember");

    @JsonValue
    private final String value;

    public static TelegramTriggerOn fromValue(String value) {
        for (TelegramTriggerOn trigger : values()) {
            if (trigger.value.equals(value)) {
                return trigger;
            }
        }
        return MESSAGE;
    }
}
