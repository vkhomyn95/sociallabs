package com.workflow.sociallabs.node.nodes.telegram.client.enums;

public enum TelegramClientTriggerEvent {
    NEW_MESSAGE,           // Нове повідомлення
    MESSAGE_EDITED,        // Редагування повідомлення
    MESSAGE_DELETED,       // Видалення повідомлення
    CHANNEL_POST,          // Пост в каналі
    CALLBACK_QUERY,        // Callback від inline кнопки
    INLINE_QUERY,          // Inline запит
    CHAT_MEMBER_UPDATED,   // Зміна учасника чату
    MY_CHAT_MEMBER,        // Зміна статусу бота в чаті
    POLL_ANSWER,           // Відповідь на опитування
    CHAT_JOIN_REQUEST,     // Запит на приєднання до чату
    ANY                    // Будь-яка подія
}
