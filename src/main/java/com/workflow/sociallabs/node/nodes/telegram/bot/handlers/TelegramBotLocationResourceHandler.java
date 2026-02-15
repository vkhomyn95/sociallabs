package com.workflow.sociallabs.node.nodes.telegram.bot.handlers;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotLocationResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendLocation(client, botToken, params, item);
            case EDIT -> executeEditLocation(client, botToken, params, item);
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
    private static Map<String, Object> executeSendLocation(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String chatId = replacePlaceholders(params.getChatId(), item);
        body.put(TelegramRequestKeys.CHAT_ID, chatId);
        body.put(TelegramRequestKeys.LATITUDE, params.getLatitude());
        body.put(TelegramRequestKeys.LONGITUDE, params.getLongitude());

        // Live location parameters
        if (params.getHorizontalAccuracy() != null) {
            body.put(TelegramRequestKeys.HORIZONTAL_ACCURACY, params.getHorizontalAccuracy());
        }
        if (params.getLivePeriod() != null) {
            body.put(TelegramRequestKeys.LIVE_PERIOD, params.getLivePeriod());
        }
        if (params.getHeading() != null) {
            body.put(TelegramRequestKeys.HEADING, params.getHeading());
        }
        if (params.getProximityAlertRadius() != null) {
            body.put(TelegramRequestKeys.PROXIMITY_ALERT_RADIUS, params.getProximityAlertRadius());
        }

        // Options
        addCommonOptions(body, params);
        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_LOCATION, body, params);
    }

    /**
     * Executor для Telegram Bot Action
     * Виконує редагування повідомлення до Telegram API
     */
    private static Map<String, Object> executeEditLocation(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {

        Map<String, Object> requestBody = new HashMap<>();

        if (params.getInlineMessageId() != null) {
            requestBody.put(TelegramRequestKeys.INLINE_MESSAGE_ID, params.getInlineMessageId());
        } else {
            String chatId = replacePlaceholders(params.getChatId(), item);
            requestBody.put(TelegramRequestKeys.CHAT_ID, chatId);
            requestBody.put(TelegramRequestKeys.MESSAGE_ID, Integer.parseInt(params.getMessageId()));
        }

        requestBody.put(TelegramRequestKeys.LATITUDE, params.getLatitude());
        requestBody.put(TelegramRequestKeys.LONGITUDE, params.getLongitude());

        if (params.getHorizontalAccuracy() != null) {
            requestBody.put(TelegramRequestKeys.HORIZONTAL_ACCURACY, params.getHorizontalAccuracy());
        }
        if (params.getHeading() != null) {
            requestBody.put(TelegramRequestKeys.HEADING, params.getHeading());
        }
        if (params.getProximityAlertRadius() != null) {
            requestBody.put(TelegramRequestKeys.PROXIMITY_ALERT_RADIUS, params.getProximityAlertRadius());
        }

        // Reply markup
        addReplyMarkup(requestBody, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.EDIT_LOCATION, requestBody, params);
    }
}
