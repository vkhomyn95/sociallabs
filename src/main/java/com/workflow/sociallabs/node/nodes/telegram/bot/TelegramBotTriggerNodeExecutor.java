package com.workflow.sociallabs.node.nodes.telegram.bot;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractTriggerNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Executor для Telegram Trigger
 * Налаштовує webhook та обробляє вхідні повідомлення
 */
@Slf4j
@Component
public class TelegramBotTriggerNodeExecutor extends AbstractTriggerNode {

    private final RestTemplate restTemplate;
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot{token}/{method}";

    // Зберігаємо активні webhook URLs
    private final Map<String, String> activeWebhooks = new HashMap<>();

    public TelegramBotTriggerNodeExecutor() {
        super(NodeDiscriminator.TELEGRAM_BOT_TRIGGER);
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean requiresCredentials() {
        return true;
    }

    /**
     * Активувати тригер - встановити Telegram webhook
     */
    @Override
    public boolean activate(ExecutionContext context) throws Exception {
//        String botToken = context.getCredential("botToken", String.class);
//        if (botToken == null) {
//            throw new IllegalStateException("Telegram bot token not found");
//        }
//
//        // Отримати або згенерувати webhook path
//        String webhookPath = context.getParameter("webhookPath", String.class);
//        if (webhookPath == null || webhookPath.isEmpty()) {
//            webhookPath = "telegram-" + context.getNodeId();
//        }
//
//        // Побудувати повний webhook URL
//        String webhookUrl = buildWebhookUrl(context.getExecutionId(), webhookPath);
//
//        // Встановити webhook в Telegram
//        boolean success = setTelegramWebhook(botToken, webhookUrl);
//
//        if (success) {
//            activeWebhooks.put(context.getNodeId(), webhookUrl);
//            log.info("Telegram webhook activated: {}", webhookUrl);
//            return true;
//        }

        return false;
    }

    /**
     * Деактивувати тригер - видалити webhook
     */
    @Override
    public void deactivate(ExecutionContext context) throws Exception {
        String botToken = context.getCredential("botToken", String.class);
        if (botToken == null) {
            return;
        }

        // Видалити webhook з Telegram
        deleteTelegramWebhook(botToken);

        activeWebhooks.remove(context.getNodeId());
        log.info("Telegram webhook deactivated for node: {}", context.getNodeId());
    }

    /**
     * Виконати тригер - обробити вхідне повідомлення
     */
    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
//        // Отримати дані webhook (передані через inputData)
//        Map<String, Object> webhookData = context.getFirstInputItem();
//
//        if (webhookData == null || webhookData.isEmpty()) {
//            return NodeResult.error("No webhook data received", null);
//        }
//
//        // Перевірити фільтри
//        if (!matchesFilters(webhookData, context)) {
//            log.debug("Message doesn't match filters, skipping");
//            return NodeResult.success(Collections.emptyList()); // Skip
//        }
//
//        // Збагатити дані
//        Map<String, Object> enrichedData = new HashMap<>(webhookData);
//        enrichedData.put("triggerTime", java.time.Instant.now().toString());
//        enrichedData.put("nodeId", context.getNodeId());

//        return NodeResult.success(enrichedData);
        return null;
    }

    /**
     * Перевірити чи повідомлення відповідає фільтрам
     */
//    private boolean matchesFilters(Map<String, Object> data, ExecutionContext context) {
//        String triggerOn = context.getParameter("triggerOn", String.class, "message");
//
//        // Перевірити тип trigger
//        if ("command".equals(triggerOn)) {
//            String command = context.getParameter("command", String.class);
//            String text = extractText(data);
//
//            if (text == null || !text.startsWith("/" + command)) {
//                return false;
//            }
//        }
//
//        // Перевірити chat ID filter
//        String chatIdFilter = context.getParameter("chatIdFilter", String.class);
//        if (chatIdFilter != null && !chatIdFilter.isEmpty()) {
//            String chatId = extractChatId(data);
//            if (!chatIdFilter.equals(chatId)) {
//                return false;
//            }
//        }
//
//        // Перевірити message types
//        @SuppressWarnings("unchecked")
//        List<String> messageTypes = context.getParameter("messageTypes", List.class);
//        if (messageTypes != null && !messageTypes.isEmpty()) {
//            String messageType = detectMessageType(data);
//            if (!messageTypes.contains(messageType)) {
//                return false;
//            }
//        }
//
//        return true;
//    }

    /**
     * Встановити webhook в Telegram
     */
    private boolean setTelegramWebhook(String botToken, String webhookUrl) {
        String url = TELEGRAM_API_URL
                .replace("{token}", botToken)
                .replace("{method}", "setWebhook");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("url", webhookUrl);
        requestBody.put("allowed_updates", Arrays.asList(
                "message", "edited_message", "channel_post",
                "edited_channel_post", "callback_query"
        ));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );

            Map<?, ?> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("ok"));

        } catch (Exception e) {
            log.error("Failed to set Telegram webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Видалити webhook з Telegram
     */
    private void deleteTelegramWebhook(String botToken) {
        String url = TELEGRAM_API_URL
                .replace("{token}", botToken)
                .replace("{method}", "deleteWebhook");

        try {
            restTemplate.postForEntity(url, null, Map.class);
        } catch (Exception e) {
            log.error("Failed to delete Telegram webhook: {}", e.getMessage());
        }
    }

    /**
     * Побудувати webhook URL
     */
    private String buildWebhookUrl(Long executionId, String path) {
        // Має брати з application.properties
        String baseUrl = "http://localhost:8080"; // TODO: inject from config
        return String.format("%s/api/v1/webhooks/%d/%s", baseUrl, executionId, path);
    }

    /**
     * Витягти текст з повідомлення
     */
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> data) {
        Map<String, Object> message = (Map<String, Object>) data.get("message");
        if (message != null) {
            return (String) message.get("text");
        }
        return null;
    }

    /**
     * Витягти chat ID
     */
    @SuppressWarnings("unchecked")
    private String extractChatId(Map<String, Object> data) {
        Map<String, Object> message = (Map<String, Object>) data.get("message");
        if (message != null) {
            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
            if (chat != null) {
                Object id = chat.get("id");
                return id != null ? id.toString() : null;
            }
        }
        return null;
    }

    /**
     * Визначити тип повідомлення
     */
    @SuppressWarnings("unchecked")
    private String detectMessageType(Map<String, Object> data) {
        Map<String, Object> message = (Map<String, Object>) data.get("message");
        if (message == null) {
            return "unknown";
        }

        if (message.containsKey("text")) return "text";
        if (message.containsKey("photo")) return "photo";
        if (message.containsKey("video")) return "video";
        if (message.containsKey("document")) return "document";
        if (message.containsKey("audio")) return "audio";
        if (message.containsKey("voice")) return "voice";
        if (message.containsKey("sticker")) return "sticker";

        return "other";
    }
}
