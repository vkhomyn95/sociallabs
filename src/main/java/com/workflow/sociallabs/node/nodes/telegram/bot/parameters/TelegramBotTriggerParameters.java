package com.workflow.sociallabs.node.nodes.telegram.bot.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramMessageType;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramTriggerOn;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Повні типізовані параметри для Telegram Bot Trigger
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName(NodeDiscriminator.Values.TELEGRAM_BOT_TRIGGER)
public class TelegramBotTriggerParameters implements TypedNodeParameters {

    // ========== Основні параметри ==========

    /**
     * На що реагувати тригер
     */
    @Builder.Default
    private TelegramTriggerOn triggerOn = TelegramTriggerOn.MESSAGE;

    /**
     * Команда для обробки (якщо triggerOn = command)
     * Без слеша, наприклад: "start", "help"
     */
    private String command;

    /**
     * Callback data pattern (якщо triggerOn = callback)
     * Може бути регулярний вираз
     */
    private String callbackData;

    /**
     * Inline query pattern (якщо triggerOn = inline)
     */
    private String inlineQuery;

    // ========== Фільтри ==========

    /**
     * Фільтр по chat ID
     * Може бути number або @username
     */
    private String chatIdFilter;

    /**
     * Фільтр по типу чату
     */
    private List<String> chatTypeFilter;

    /**
     * Фільтр по типам повідомлень
     */
    private List<TelegramMessageType> messageTypes;

    /**
     * Фільтр по user ID відправника
     */
    private String userIdFilter;

    /**
     * Фільтр по username відправника (з @)
     */
    private String usernameFilter;

    /**
     * Ігнорувати старі повідомлення (за замовчуванням true)
     */
    @Builder.Default
    private Boolean ignoreOldMessages = true;

    /**
     * Максимальний вік повідомлення в секундах для обробки
     */
    @Builder.Default
    private Integer maxMessageAge = 60;

    // ========== Text Filters ==========

    /**
     * Фільтр по тексту (contains)
     */
    private String textContains;

    /**
     * Фільтр по тексту (regex)
     */
    private String textRegex;

    /**
     * Фільтр по початку тексту
     */
    private String textStartsWith;

    /**
     * Фільтр по кінцю тексту
     */
    private String textEndsWith;

    /**
     * Case sensitive для текстових фільтрів
     */
    @Builder.Default
    private Boolean caseSensitive = false;

    // ========== Media Filters ==========

    /**
     * Фільтр по розміру файлу (max bytes)
     */
    private Long maxFileSize;

    /**
     * Фільтр по MIME type
     */
    private List<String> mimeTypes;

    /**
     * Фільтр по розширенню файлу
     */
    private List<String> fileExtensions;

    // ========== Additional Filters ==========

    /**
     * Фільтр тільки для нових членів групи
     */
    @Builder.Default
    private Boolean onlyNewMembers = false;

    /**
     * Фільтр тільки для членів, що покинули групу
     */
    @Builder.Default
    private Boolean onlyLeftMembers = false;

    /**
     * Фільтр тільки для закріплених повідомлень
     */
    @Builder.Default
    private Boolean onlyPinnedMessages = false;

    /**
     * Фільтр тільки для forwarded повідомлень
     */
    @Builder.Default
    private Boolean onlyForwardedMessages = false;

    /**
     * Фільтр тільки для reply повідомлень
     */
    @Builder.Default
    private Boolean onlyReplyMessages = false;

    /**
     * Ігнорувати повідомлення від ботів
     */
    @Builder.Default
    private Boolean ignoreBots = true;

    /**
     * Ігнорувати повідомлення в групах
     */
    @Builder.Default
    private Boolean ignoreGroupMessages = false;

    /**
     * Ігнорувати повідомлення в каналах
     */
    @Builder.Default
    private Boolean ignoreChannelMessages = false;

    // ========== Webhook Settings ==========

    /**
     * Custom webhook path
     */
    private String webhookPath;

    /**
     * Secret token для верифікації webhook
     */
    private String secretToken;

    /**
     * IP адреса для webhook (для додаткової безпеки)
     */
    private String ipAddress;

    /**
     * Максимальна кількість з'єднань для webhook
     */
    @Builder.Default
    private Integer maxConnections = 40;

    /**
     * Allowed updates для webhook
     */
    private List<String> allowedUpdates;

    /**
     * Drop pending updates при встановленні webhook
     */
    @Builder.Default
    private Boolean dropPendingUpdates = false;

    // ========== Output Settings ==========

    /**
     * Включити raw data в output
     */
    @Builder.Default
    private Boolean includeRawData = false;

    /**
     * Включити user profile в output
     */
    @Builder.Default
    private Boolean includeUserProfile = false;

    /**
     * Включити chat info в output
     */
    @Builder.Default
    private Boolean includeChatInfo = false;

    /**
     * Додаткові поля для output
     */
    private List<String> additionalFields;

    // ========== Error Handling ==========

    /**
     * Чи продовжувати при помилці
     */
    @Builder.Default
    private Boolean continueOnFail = false;

    /**
     * Максимальна кількість повторів
     */
    @Builder.Default
    private Integer retryAttempts = 3;

    /**
     * Затримка між повторами (секунди)
     */
    @Builder.Default
    private Integer retryDelay = 5;

    // ========== Methods ==========

    @Override
    public void validate() throws IllegalArgumentException {
        if (triggerOn == null) {
            throw new IllegalArgumentException("triggerOn is required");
        }

        // Валідація для command trigger
        if (triggerOn == TelegramTriggerOn.COMMAND) {
            if (command == null || command.trim().isEmpty()) {
                throw new IllegalArgumentException("command is required when triggerOn is 'command'");
            }
            // Видалити слеш якщо є
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
        }

        // Валідація для callback trigger
        if (triggerOn == TelegramTriggerOn.CALLBACK_QUERY) {
            if (callbackData == null || callbackData.trim().isEmpty()) {
                throw new IllegalArgumentException("callbackData is required when triggerOn is 'callback'");
            }
        }

        // Валідація для inline trigger
        if (triggerOn == TelegramTriggerOn.INLINE_QUERY) {
            if (inlineQuery == null || inlineQuery.trim().isEmpty()) {
                throw new IllegalArgumentException("inlineQuery is required when triggerOn is 'inline'");
            }
        }

        // Валідація maxMessageAge
        if (maxMessageAge != null && maxMessageAge < 0) {
            throw new IllegalArgumentException("maxMessageAge must be positive");
        }

        // Валідація maxConnections
        if (maxConnections != null && (maxConnections < 1 || maxConnections > 100)) {
            throw new IllegalArgumentException("maxConnections must be between 1 and 100");
        }

        // Валідація maxFileSize
        if (maxFileSize != null && maxFileSize < 0) {
            throw new IllegalArgumentException("maxFileSize must be positive");
        }
    }

    @Override
    public NodeDiscriminator getParameterType() {
        return NodeDiscriminator.TELEGRAM_BOT_TRIGGER;
    }

    // ========== Helper Methods ==========

    /**
     * Чи тригер на команду
     */
    public boolean isCommandTrigger() {
        return triggerOn == TelegramTriggerOn.COMMAND;
    }

    /**
     * Чи тригер на повідомлення
     */
    public boolean isMessageTrigger() {
        return triggerOn == TelegramTriggerOn.MESSAGE;
    }

    /**
     * Чи тригер на callback
     */
    public boolean isCallbackTrigger() {
        return triggerOn == TelegramTriggerOn.CALLBACK_QUERY;
    }

    /**
     * Чи тригер на inline query
     */
    public boolean isInlineTrigger() {
        return triggerOn == TelegramTriggerOn.INLINE_QUERY;
    }

    /**
     * Чи є фільтр по chat ID
     */
    public boolean hasChatIdFilter() {
        return chatIdFilter != null && !chatIdFilter.trim().isEmpty();
    }

    /**
     * Чи є фільтр по типу повідомлення
     */
    public boolean hasMessageTypeFilter() {
        return messageTypes != null && !messageTypes.isEmpty();
    }

    /**
     * Чи є фільтр по user ID
     */
    public boolean hasUserIdFilter() {
        return userIdFilter != null && !userIdFilter.trim().isEmpty();
    }

    /**
     * Чи є фільтр по username
     */
    public boolean hasUsernameFilter() {
        return usernameFilter != null && !usernameFilter.trim().isEmpty();
    }

    /**
     * Чи є текстові фільтри
     */
    public boolean hasTextFilters() {
        return (textContains != null && !textContains.isEmpty()) ||
                (textRegex != null && !textRegex.isEmpty()) ||
                (textStartsWith != null && !textStartsWith.isEmpty()) ||
                (textEndsWith != null && !textEndsWith.isEmpty());
    }

    /**
     * Чи є медіа фільтри
     */
    public boolean hasMediaFilters() {
        return maxFileSize != null ||
                (mimeTypes != null && !mimeTypes.isEmpty()) ||
                (fileExtensions != null && !fileExtensions.isEmpty());
    }

    /**
     * Отримати нормалізовану команду (без слеша)
     */
    public String getNormalizedCommand() {
        if (command == null) {
            return null;
        }
        return command.startsWith("/") ? command.substring(1) : command;
    }

    /**
     * Чи є custom allowed updates
     */
    public boolean hasCustomAllowedUpdates() {
        return allowedUpdates != null && !allowedUpdates.isEmpty();
    }

    /**
     * Чи потрібно завантажувати додаткову інформацію
     */
    public boolean needsAdditionalInfo() {
        return includeUserProfile || includeChatInfo || (additionalFields != null && !additionalFields.isEmpty());
    }
}

