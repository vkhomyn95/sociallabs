package com.workflow.sociallabs.node.nodes.telegram.bot.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Тип ресурсу для Telegram Bot
 */
@Getter
@RequiredArgsConstructor
public enum TelegramResource {
    MESSAGE("message"),
    PHOTO("photo"),
    VIDEO("video"),
    DOCUMENT("document"),
    AUDIO("audio"),
    STICKER("sticker"),
    LOCATION("location"),
    CONTACT("contact"),
    VENUE("venue"),
    ANIMATION("animation"),
    VOICE("voice"),
    VIDEO_NOTE("videoNote"),
    MEDIA_GROUP("mediaGroup"),
    POLL("poll"),
    DICE("dice");

    @JsonValue
    private final String value;

    public static TelegramResource fromValue(String value) {
        for (TelegramResource resource : values()) {
            if (resource.value.equals(value)) {
                return resource;
            }
        }
        return MESSAGE; // default
    }
}
