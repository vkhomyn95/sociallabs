package com.workflow.sociallabs.node.nodes.telegram.bot.handlers;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramAttachmentType;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotVideoResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendVideo(client, botToken, params, item);
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
    private static Map<String, Object> executeSendVideo(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String chatId = replacePlaceholders(params.getChatId(), item);
        body.put(TelegramRequestKeys.CHAT_ID, chatId);

        // Video
        if (params.getAttachmentType() == TelegramAttachmentType.URL) {
            String videoUrl = replacePlaceholders(params.getVideoUrl(), item);
            body.put(TelegramRequestKeys.VIDEO, videoUrl);
        } else if (params.getAttachmentType() == TelegramAttachmentType.FILE_ID) {
            body.put(TelegramRequestKeys.VIDEO, params.getVideoFileId());
        }

        // Caption
        addCaption(body, params, item);

        // Video parameters
        if (params.getDuration() != null) body.put(TelegramRequestKeys.DURATION, params.getDuration());
        if (params.getWidth() != null) body.put(TelegramRequestKeys.WIDTH, params.getWidth());
        if (params.getHeight() != null) body.put(TelegramRequestKeys.HEIGHT, params.getHeight());
        if (params.getThumbnailUrl() != null) body.put(TelegramRequestKeys.THUMBNAIL, params.getThumbnailUrl());
        if (params.getSupportsStreaming()) body.put(TelegramRequestKeys.SUPPORTS_STREAMING, true);

        // Options
        addCommonOptions(body, params);

        if (params.getHasSpoiler()) body.put(TelegramRequestKeys.HAS_SPOILER, true);

        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_VIDEO, body, params);
    }
}
