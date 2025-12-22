package com.workflow.sociallabs.node.nodes.telegram.bot.handler;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotVenueResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendVenue(client, botToken, params, item);
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
    @SuppressWarnings("Duplicates")
    private static Map<String, Object> executeSendVenue(
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
        body.put(TelegramRequestKeys.TITLE, replacePlaceholders(params.getVenueName(), item));
        body.put(TelegramRequestKeys.ADDRESS, replacePlaceholders(params.getAddress(), item));

        if (params.getFoursquareId() != null) {
            body.put(TelegramRequestKeys.FOURSQUARE_ID, params.getFoursquareId());
        }
        if (params.getFoursquareType() != null) {
            body.put(TelegramRequestKeys.FOURSQUARE_TYPE, params.getFoursquareType());
        }
        if (params.getGooglePlaceId() != null) {
            body.put(TelegramRequestKeys.GOOGLE_PLACE_ID, params.getGooglePlaceId());
        }
        if (params.getGooglePlaceType() != null) {
            body.put(TelegramRequestKeys.GOOGLE_PLACE_TYPE, params.getGooglePlaceType());
        }

        // Options
        addCommonOptions(body, params);
        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_VENUE, body, params);
    }
}
