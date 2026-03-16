package com.workflow.sociallabs.node.nodes.telegram.client.handlers;

import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class TelegramClientBaseHandler {

    /**
     * Виконати запит до Telegram через TDLight
     */
    protected static <T extends TdApi.Object> T sendTelegramRequest(
            SimpleTelegramClient client,
            TdApi.Function<T> request,
            TelegramClientActionParameters params
    ) throws Exception {
        try {
            CompletableFuture<T> future = client.send(request);

            T response = future.get(params.getRequestTimeout(), TimeUnit.SECONDS);

            if (response instanceof TdApi.Error error) {
                throw new RuntimeException("Telegram API Error: " + error.message + " (code: " + error.code + ")");
            }

            return response;
        } catch (Exception e) {
            log.error("Error executing Telegram request: {}", e.getMessage());
            throw new RuntimeException("Failed to execute Telegram action: " + e.getMessage(), e);
        }
    }

    /**
     * Замінити плейсхолдери типу {{field}} на значення з item
     */
    protected static String replacePlaceholders(String text, Map<String, Object> item) {
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
     * Парсити chat ID (може бути число, @username, або змінна)
     */
    protected static long parseChatId(String chatIdStr, Map<String, Object> item) {
        String resolved = replacePlaceholders(chatIdStr, item);

        // Якщо username (@username)
        if (resolved.startsWith("@")) {
            throw new IllegalArgumentException("Username resolution not yet implemented. Please use numeric chat ID.");
        }

        // Інакше - це число
        try {
            return Long.parseLong(resolved);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid chat ID format: " + resolved);
        }
    }

    /**
     * Створити InputMessageContent для текстового повідомлення
     */
    protected static TdApi.InputMessageText createInputMessageText(
            String text,
            TelegramClientActionParameters params,
            Map<String, Object> item
    ) {
        String resolvedText = replacePlaceholders(text, item);

        TdApi.FormattedText formattedText = new TdApi.FormattedText();
        formattedText.text = resolvedText;
        formattedText.entities = new TdApi.TextEntity[0];

        TdApi.LinkPreviewOptions previewOptions = new TdApi.LinkPreviewOptions();
        previewOptions.isDisabled = params.getDisableWebPagePreview();
        previewOptions.url = null;
        previewOptions.forceSmallMedia = false;
        previewOptions.forceLargeMedia = false;
        previewOptions.showAboveText = false;

        TdApi.InputMessageText inputMessage = new TdApi.InputMessageText();
        inputMessage.text = formattedText;
        inputMessage.linkPreviewOptions = previewOptions;
        inputMessage.clearDraft = params.getClearDraft();

        return inputMessage;
    }

    /**
     * Створити MessageSendOptions
     */
    protected static TdApi.MessageSendOptions createSendOptions(TelegramClientActionParameters params) {
        TdApi.MessageSendOptions options = new TdApi.MessageSendOptions();
        options.disableNotification = params.getDisableNotification();
        options.fromBackground = false;
        options.protectContent = params.getProtectContent();
        options.updateOrderOfInstalledStickerSets = false;

        if (params.getSchedulingState() != null) {
            options.schedulingState = new TdApi.MessageSchedulingStateSendAtDate(params.getSchedulingState());
        } else {
            options.schedulingState = null;
        }

        return options;
    }

    /**
     * Створити ReplyMarkup
     */
    protected static TdApi.ReplyMarkup createReplyMarkup(TelegramClientActionParameters params) {
        if (!params.hasButtons() && !params.hasReplyMarkup()) {
            return null;
        }

        return switch (params.getReplyMarkupType()) {
            case INLINE -> createInlineKeyboard(params);
            case KEYBOARD -> createReplyKeyboard(params);
            case REMOVE -> new TdApi.ReplyMarkupRemoveKeyboard(params.getSelective());
            case FORCE_REPLY -> {
                TdApi.ReplyMarkupForceReply forceReply = new TdApi.ReplyMarkupForceReply();
                forceReply.inputFieldPlaceholder = params.getInputFieldPlaceholder();
                forceReply.isPersonal = params.getSelective();
                yield forceReply;
            }
        };
    }

    private static TdApi.ReplyMarkupInlineKeyboard createInlineKeyboard(TelegramClientActionParameters params) {
        if (params.getButtons() == null || params.getButtons().isEmpty()) {
            return null;
        }

        TdApi.InlineKeyboardButton[][] keyboard = new TdApi.InlineKeyboardButton[params.getButtons().size()][];

        for (int i = 0; i < params.getButtons().size(); i++) {
            List<Map<String, Object>> row = params.getButtons().get(i);
            keyboard[i] = new TdApi.InlineKeyboardButton[row.size()];

            for (int j = 0; j < row.size(); j++) {
                Map<String, Object> button = row.get(j);
                keyboard[i][j] = createInlineButton(button);
            }
        }

        return new TdApi.ReplyMarkupInlineKeyboard(keyboard);
    }

    private static TdApi.InlineKeyboardButton createInlineButton(Map<String, Object> buttonData) {
        String text = (String) buttonData.get("text");
        TdApi.InlineKeyboardButtonType type = null;

        if (buttonData.containsKey("url")) {
            type = new TdApi.InlineKeyboardButtonTypeUrl((String) buttonData.get("url"));
        } else if (buttonData.containsKey("callback_data")) {
            type = new TdApi.InlineKeyboardButtonTypeCallback(
                    ((String) buttonData.get("callback_data")).getBytes()
            );
        } else if (buttonData.containsKey("switch_inline_query")) {
            // TODO: 22.12.25 ==========================
//            type = new TdApi.InlineKeyboardButtonTypeSwitchInline(
//                    (String) buttonData.get("switch_inline_query"),
//                    false
//            );
        } else {
            throw new IllegalArgumentException("Invalid button type");
        }

        return new TdApi.InlineKeyboardButton(text, type);
    }

    private static TdApi.ReplyMarkupShowKeyboard createReplyKeyboard(TelegramClientActionParameters params) {
        if (params.getButtons() == null || params.getButtons().isEmpty()) {
            return null;
        }

        TdApi.KeyboardButton[][] keyboard = new TdApi.KeyboardButton[params.getButtons().size()][];

        for (int i = 0; i < params.getButtons().size(); i++) {
            List<Map<String, Object>> row = params.getButtons().get(i);
            keyboard[i] = new TdApi.KeyboardButton[row.size()];

            for (int j = 0; j < row.size(); j++) {
                Map<String, Object> button = row.get(j);
                keyboard[i][j] = createKeyboardButton(button);
            }
        }

        TdApi.ReplyMarkupShowKeyboard replyKeyboard = new TdApi.ReplyMarkupShowKeyboard();
        replyKeyboard.rows = keyboard;
        replyKeyboard.isPersistent = !params.getOneTimeKeyboard();
        replyKeyboard.resizeKeyboard = params.getResizeKeyboard();
        replyKeyboard.oneTime = params.getOneTimeKeyboard();
        replyKeyboard.isPersonal = params.getSelective();
        replyKeyboard.inputFieldPlaceholder = params.getInputFieldPlaceholder();

        return replyKeyboard;
    }

    private static TdApi.KeyboardButton createKeyboardButton(Map<String, Object> buttonData) {
        String text = (String) buttonData.get("text");
        TdApi.KeyboardButtonType type;

        if (buttonData.containsKey("request_contact") && (Boolean) buttonData.get("request_contact")) {
            type = new TdApi.KeyboardButtonTypeRequestPhoneNumber();
        } else if (buttonData.containsKey("request_location") && (Boolean) buttonData.get("request_location")) {
            type = new TdApi.KeyboardButtonTypeRequestLocation();
        } else {
            type = new TdApi.KeyboardButtonTypeText();
        }

        return new TdApi.KeyboardButton(text, type);
    }

    /**
     * Конвертувати TdApi.Message в Map для результату
     */
    protected static Map<String, Object> messageToMap(TdApi.Message message) {
        Map<String, Object> result = new HashMap<>();
        result.put("message_id", message.id);
        result.put("chat_id", message.chatId);
        result.put("date", message.date);
        result.put("sender_id", message.senderId);
        result.put("is_outgoing", message.isOutgoing);

        // TODO: 22.12.25 ==========================
//        result.put("can_be_edited", message.canBeEdited);
//        result.put("can_be_deleted_only_for_self", message.canBeDeletedOnlyForSelf);
//        result.put("can_be_deleted_for_all_users", message.canBeDeletedForAllUsers);

        // Content info
        if (message.content != null) {
            result.put("content_type", message.content.getClass().getSimpleName());
        }

        return result;
    }
}