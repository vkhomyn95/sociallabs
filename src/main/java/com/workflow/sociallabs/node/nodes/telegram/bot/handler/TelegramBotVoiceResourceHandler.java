package com.workflow.sociallabs.node.nodes.telegram.bot.handler;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramAttachmentType;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotVoiceResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendVoice(client, botToken, params, item);
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
    private static Map<String, Object> executeSendVoice(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String chatId = replacePlaceholders(params.getChatId(), item);
        body.put(TelegramRequestKeys.CHAT_ID, chatId);

        // Audio
        if (params.getAttachmentType() == TelegramAttachmentType.URL) {
            String audioUrl = replacePlaceholders(params.getAudioUrl(), item);
            body.put(TelegramRequestKeys.VOICE, audioUrl);
        } else if (params.getAttachmentType() == TelegramAttachmentType.FILE_ID) {
            body.put(TelegramRequestKeys.VOICE, params.getAudioFileId());
        }

        // Caption
        addCaption(body, params, item);

        // Duration
        if (params.getDuration() != null) body.put(TelegramRequestKeys.DURATION, params.getDuration());

        // Options
        addCommonOptions(body, params);
        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_VOICE, body, params);
    }
}
