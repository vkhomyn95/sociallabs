
package com.workflow.sociallabs.node.nodes.telegram.bot.handlers;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResourceKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class TelegramBotPollResourceHandler extends TelegramBotBaseResourceHandler {

    @SuppressWarnings("Duplicates")
    public static Map<String, Object> execute(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        return switch (params.getOperation()) {
            case SEND -> executeSendPoll(client, botToken, params, item);
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
    private static Map<String, Object> executeSendPoll(
            WebClient client,
            String botToken,
            TelegramBotActionParameters params,
            Map<String, Object> item
    ) throws Exception {
        Map<String, Object> body = new HashMap<>();

        String chatId = replacePlaceholders(params.getChatId(), item);
        body.put(TelegramRequestKeys.CHAT_ID, chatId);

        body.put(TelegramRequestKeys.QUESTION, replacePlaceholders(params.getQuestion(), item));
        body.put(TelegramRequestKeys.OPTIONS, params.getPollOptions());

        if (params.getIsAnonymous() != null) {
            body.put(TelegramRequestKeys.IS_ANONYMOUS, params.getIsAnonymous());
        }
        if (params.getPollType() != null) {
            body.put(TelegramRequestKeys.TYPE, params.getPollType());
        }
        if (params.getAllowsMultipleAnswers() != null) {
            body.put(TelegramRequestKeys.ALLOWS_MULTIPLE_ANSWERS, params.getAllowsMultipleAnswers());
        }
        if (params.getCorrectOptionId() != null) {
            body.put(TelegramRequestKeys.CORRECT_OPTION_ID, params.getCorrectOptionId());
        }
        if (params.getExplanation() != null) {
            body.put(TelegramRequestKeys.EXPLANATION, params.getExplanation());
        }
        if (params.getExplanationParseMode() != null) {
            body.put(TelegramRequestKeys.EXPLANATION_PARSE_MODE, params.getExplanationParseMode().getTelegramValue());
        }
        if (params.getOpenPeriod() != null) {
            body.put(TelegramRequestKeys.OPEN_PERIOD, params.getOpenPeriod());
        }
        if (params.getCloseDate() != null) {
            body.put(TelegramRequestKeys.CLOSE_DATE, params.getCloseDate());
        }
        if (params.getIsClosed() != null) {
            body.put(TelegramRequestKeys.IS_CLOSED, params.getIsClosed());
        }

        // Options
        addCommonOptions(body, params);

        // Reply markup
        addReplyMarkup(body, params);

        return sendTelegramRequest(client, botToken, TelegramResourceKeys.SEND_POLL, body, params);
    }
}
