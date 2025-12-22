package com.workflow.sociallabs.node.nodes.telegram.bot.handler;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramAttachmentType;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotPhotoResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendPhoto(client, botToken, params, item);
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
    private static Map<String, Object> executeSendPhoto(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String chatId = replacePlaceholders(params.getChatId(), item);
        body.put(TelegramRequestKeys.CHAT_ID, chatId);

        // Photo (URL або file_id)
        if (params.getAttachmentType() == TelegramAttachmentType.URL) {
            String photoUrl = replacePlaceholders(params.getPhotoUrl(), item);
            body.put(TelegramRequestKeys.PHOTO, photoUrl);
        } else if (params.getAttachmentType() == TelegramAttachmentType.FILE_ID) {
            body.put(TelegramRequestKeys.PHOTO, params.getPhotoFileId());
        }

        // Caption
        addCaption(body, params, item);

        // Options
        addCommonOptions(body, params);

        if (params.getHasSpoiler()) body.put(TelegramRequestKeys.HAS_SPOILER, true);

        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_PHOTO, body, params);
    }
}
