package com.workflow.sociallabs.node.nodes.telegram.bot.handler;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotMessageResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendMessage(client, botToken, params, item);
            case EDIT -> executeEditMessage(client, botToken, params, item);
            case DELETE -> executeDeleteMessage(client, botToken, params, item);
            case PIN -> executePinMessage(client, botToken, params, item);
            case UNPIN -> executeUnpinMessage(client, botToken, params, item);
            case FORWARD -> executeForwardMessage(client, botToken, params, item);
            default -> throw new IllegalArgumentException("Unsupported operation for message: " + params.getOperation());
        };
    }

    /**
     * Executor для Telegram Bot Action
     * Виконує надсилання сирого повідомлення до Telegram API
     */
    private static Map<String, Object> executeSendMessage(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String chatId = replacePlaceholders(params.getChatId(), item);
        String text = replacePlaceholders(params.getText(), item);

        body.put(TelegramRequestKeys.CHAT_ID, chatId);
        body.put(TelegramRequestKeys.TEXT, text);

        // Parse mode
        if (params.getEffectiveParseMode() != null) {
            body.put(TelegramRequestKeys.PARSE_MODE, params.getEffectiveParseMode());
        }

        // Entities
        if (params.getEntities() != null && !params.getEntities().isEmpty()) {
            body.put(TelegramRequestKeys.ENTITIES, params.getEntities());
        }

        // Options
        addCommonOptions(body, params);

        // Web page preview
        if (params.getDisableWebPagePreview()) body.put(TelegramRequestKeys.DISABLE_WEB_PAGE_PREVIEW, true);

        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_MESSAGE, body, params);
    }

    /**
     * Executor для Telegram Bot Action
     * Виконує надсилання редагованого повідомлення до Telegram API
     */
    private static Map<String, Object> executeEditMessage(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String inlineId = params.getInlineMessageId();

        if (inlineId != null && !inlineId.isEmpty()) body.put(TelegramRequestKeys.INLINE_MESSAGE_ID, inlineId);

        if (inlineId == null || inlineId.isEmpty()) {
            String chatId = replacePlaceholders(params.getChatId(), item);
            body.put(TelegramRequestKeys.CHAT_ID, chatId);
            body.put(TelegramRequestKeys.MESSAGE_ID, Integer.parseInt(params.getMessageId()));
        }

        String text = replacePlaceholders(params.getText(), item);
        body.put(TelegramRequestKeys.TEXT, text);

        // Parse mode
        if (params.getEffectiveParseMode() != null) {
            body.put(TelegramRequestKeys.PARSE_MODE, params.getEffectiveParseMode());
        }

        // Web page preview
        if (params.getDisableWebPagePreview()) body.put(TelegramRequestKeys.DISABLE_WEB_PAGE_PREVIEW, true);

        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.EDIT_MESSAGE_TEXT, body, params);
    }
}
