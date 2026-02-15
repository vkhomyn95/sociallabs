package com.workflow.sociallabs.node.nodes.telegram.bot.handlers;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramAttachmentType;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotDocumentResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendDocument(client, botToken, params, item);
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
    private static Map<String, Object> executeSendDocument(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String chatId = replacePlaceholders(params.getChatId(), item);
        body.put(TelegramRequestKeys.CHAT_ID, chatId);

        // Document
        if (params.getAttachmentType() == TelegramAttachmentType.URL) {
            String documentUrl = replacePlaceholders(params.getDocumentUrl(), item);
            body.put(TelegramRequestKeys.DOCUMENT, documentUrl);
        } else if (params.getAttachmentType() == TelegramAttachmentType.FILE_ID) {
            body.put(TelegramRequestKeys.DOCUMENT, params.getDocumentFileId());
        }

        // Caption
        addCaption(body, params, item);

        // Options
        addCommonOptions(body, params);

        if (params.getDisableContentTypeDetection()) body.put(TelegramRequestKeys.DISABLE_CONTENT_TYPE_DETECTION, true);

        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_DOCUMENT, body, params);
    }
}
