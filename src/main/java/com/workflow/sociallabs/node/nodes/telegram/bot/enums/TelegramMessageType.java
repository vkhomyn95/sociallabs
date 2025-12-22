package com.workflow.sociallabs.node.nodes.telegram.bot.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Тип повідомлення для фільтрації
 */
@Getter
@RequiredArgsConstructor
public enum TelegramMessageType {
    TEXT("text"),
    PHOTO("photo"),
    VIDEO("video"),
    DOCUMENT("document"),
    AUDIO("audio"),
    VOICE("voice"),
    STICKER("sticker"),
    ANIMATION("animation"),
    LOCATION("location"),
    CONTACT("contact"),
    POLL("poll"),
    DICE("dice"),
    NEW_CHAT_MEMBERS("newChatMembers"),
    LEFT_CHAT_MEMBER("leftChatMember"),
    NEW_CHAT_TITLE("newChatTitle"),
    NEW_CHAT_PHOTO("newChatPhoto"),
    DELETE_CHAT_PHOTO("deleteChatPhoto"),
    GROUP_CHAT_CREATED("groupChatCreated"),
    SUPERGROUP_CHAT_CREATED("supergroupChatCreated"),
    CHANNEL_CHAT_CREATED("channelChatCreated"),
    PINNED_MESSAGE("pinnedMessage");

    @JsonValue
    private final String value;
}