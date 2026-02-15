package com.workflow.sociallabs.node.nodes.telegram.bot.handlers;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotBaseResourceHandler extends TelegramBotResourceHandler {

    /**
     * Executor для Telegram Bot Action
     * Виконує надсилання видалення повідомлення до Telegram API
     */
    static Map<String, Object> executeDeleteMessage(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {

        String chatId = replacePlaceholders(params.getChatId(), item);

        Map<String, Object> body = new HashMap<>();
        body.put(TelegramRequestKeys.CHAT_ID, chatId);
        body.put(TelegramRequestKeys.MESSAGE_ID, Integer.parseInt(params.getMessageId()));

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.DELETE_MESSAGE, body, params);
    }

    /**
     * Executor для Telegram Bot Action
     * Виконує надсилання прикріпити повідомлення до Telegram API
     */
    static Map<String, Object> executePinMessage(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {

        String chatId = replacePlaceholders(params.getChatId(), item);

        Map<String, Object> body = new HashMap<>();
        body.put(TelegramRequestKeys.CHAT_ID, chatId);
        body.put(TelegramRequestKeys.MESSAGE_ID, Integer.parseInt(params.getMessageId()));

        if (params.getDisablePinNotification()) body.put(TelegramRequestKeys.DISABLE_NOTIFICATION, true);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.PIN_MESSAGE, body, params);
    }

    /**
     * Executor для Telegram Bot Action
     * Виконує надсилання відкріпити повідомлення до Telegram API
     */
    static Map<String, Object> executeUnpinMessage(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {

        String chatId = replacePlaceholders(params.getChatId(), item);

        Map<String, Object> body = new HashMap<>();
        body.put(TelegramRequestKeys.CHAT_ID, chatId);

        if (params.getMessageId() != null) {
            body.put(TelegramRequestKeys.MESSAGE_ID, Integer.parseInt(params.getMessageId()));
        }

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.UNPIN_MESSAGE, body, params);
    }

    /**
     * Executor для Telegram Bot Action
     * Виконує надсилання переслати повідомлення до Telegram API
     */
    static Map<String, Object> executeForwardMessage(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {

        String chatId = replacePlaceholders(params.getChatId(), item);
        String fromChatId = replacePlaceholders(params.getFromChatId(), item);

        Map<String, Object> body = new HashMap<>();
        body.put(TelegramRequestKeys.CHAT_ID, chatId);
        body.put(TelegramRequestKeys.MESSAGE_ID, Integer.parseInt(params.getMessageId()));
        body.put(TelegramRequestKeys.FROM_CHAT_ID, fromChatId);

        if (params.getDisablePinNotification()) body.put(TelegramRequestKeys.DISABLE_NOTIFICATION, true);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.FORWARD_MESSAGE, body, params);
    }
}
