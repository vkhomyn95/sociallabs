package com.workflow.sociallabs.node.nodes.telegram.bot.handlers;

import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramRequestKeys;
import com.workflow.sociallabs.node.nodes.telegram.bot.parameters.TelegramBotActionParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TelegramBotResourceHandler {

    /**
     * Виконати HTTP запит до Telegram API
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> sendTelegramRequest(
            WebClient client,
            String botToken,
            String method,
            Map<String, Object> body,
            TelegramBotActionParameters params
    ) throws Exception {
        try {
            return client.post()
                    .uri("bot{token}/{method}", botToken, method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.APPLICATION_JSON))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(params.getRequestTimeout()))
                    .retryWhen(
                            Retry.fixedDelay(
                                    params.getRetryAttempts(),
                                    Duration.ofMillis(params.getDelayBetweenRequests()))
                    )
                    .map(response -> {
                        if (Boolean.FALSE.equals(response.get("ok"))) {
                            throw new RuntimeException("Telegram API Error: " + response.get("description"));
                        }
                        // Можна повертати лише result, або всю відповідь
                        return (Map<String, Object>) response.get("result");
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error executing Telegram request: {}", e.getMessage());
            throw new RuntimeException("Failed to execute Telegram action: " + e.getMessage(), e);
        }
    }

    /**
     * Замінити плейсхолдери типу {{field}} на значення з item
     */
    static String replacePlaceholders(String text, Map<String, Object> item) {
        if (text == null || item == null || item.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, Object> entry : item.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }

        return result;
    }

    /**
     * Додати загальні опції (disable_notification, protect_content, reply_to)
     */
    static void addCommonOptions(Map<String, Object> requestBody, TelegramBotActionParameters params) {
        if (params.getDisableNotification()) requestBody.put(TelegramRequestKeys.DISABLE_NOTIFICATION, true);

        if (params.getProtectContent()) requestBody.put(TelegramRequestKeys.PROTECT_CONTENT, true);

        if (params.getReplyToMessageId() != null && !params.getReplyToMessageId().isEmpty()) {
            try {
                requestBody.put(TelegramRequestKeys.REPLY_TO_MESSAGE_ID, Integer.parseInt(params.getReplyToMessageId()));
            } catch (NumberFormatException e) {
                log.warn("Invalid replyToMessageId: {}", params.getReplyToMessageId());
            }
        }

        if (params.getAllowSendingWithoutReply()) {
            requestBody.put(TelegramRequestKeys.ALLOW_SENDING_WITHOUT_REPLY, true);
        }
    }

    static void addReplyMarkup(Map<String, Object> requestBody, TelegramBotActionParameters params) {
        if (params.hasReplyMarkup()) {
            requestBody.put(TelegramRequestKeys.REPLY_MARKUP, params.getReplyMarkup());
        } else if (params.hasButtons()) {
            // Створити inline keyboard з buttons
            Map<String, Object> replyMarkup = new HashMap<>();
            replyMarkup.put(TelegramRequestKeys.INLINE_KEYBOARD, params.getButtons());
            requestBody.put(TelegramRequestKeys.REPLY_MARKUP, replyMarkup);
        }
    }

    /**
     * Додати caption з parse mode
     */
    static void addCaption(Map<String, Object> requestBody, TelegramBotActionParameters params, Map<String, Object> item) {
        if (params.getCaption() != null && !params.getCaption().isEmpty()) {
            String caption = replacePlaceholders(params.getCaption(), item);
            requestBody.put(TelegramRequestKeys.CAPTION, caption);

            String captionParseMode = params.getEffectiveCaptionParseMode();
            if (captionParseMode != null) {
                requestBody.put(TelegramRequestKeys.PARSE_MODE, captionParseMode);
            }

            if (params.getCaptionEntities() != null && !params.getCaptionEntities().isEmpty()) {
                requestBody.put(TelegramRequestKeys.CAPTION_ENTITIES, params.getCaptionEntities());
            }
        }
    }
}
