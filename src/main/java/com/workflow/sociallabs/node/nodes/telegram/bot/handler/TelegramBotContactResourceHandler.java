package com.workflow.sociallabs.node.nodes.telegram.bot.handler;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotContactResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendContact(client, botToken, params, item);
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
    private static Map<String, Object> executeSendContact(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String chatId = replacePlaceholders(params.getChatId(), item);
        body.put(TelegramRequestKeys.CHAT_ID, chatId);

        body.put(TelegramRequestKeys.PHONE_NUMBER, replacePlaceholders(params.getPhoneNumber(), item));
        body.put(TelegramRequestKeys.FIRST_NAME, replacePlaceholders(params.getFirstName(), item));

        if (params.getLastName() != null) {
            body.put(TelegramRequestKeys.LAST_NAME, replacePlaceholders(params.getLastName(), item));
        }
        if (params.getVcard() != null) {
            body.put(TelegramRequestKeys.VCARD, replacePlaceholders(params.getVcard(), item));
        }

        // Options
        addCommonOptions(body, params);

        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_CONTACT, body, params);
    }
}
