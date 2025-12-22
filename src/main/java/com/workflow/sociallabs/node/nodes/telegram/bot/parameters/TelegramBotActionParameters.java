package com.workflow.sociallabs.node.nodes.telegram.bot.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramAttachmentType;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramOperation;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramParseMode;
import com.workflow.sociallabs.node.nodes.telegram.bot.enums.TelegramResource;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Типізовані параметри для Telegram Bot Action
 * Jackson автоматично serialize/deserialize всі поля
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName(NodeDiscriminator.Values.TELEGRAM_BOT_ACTION)
public class TelegramBotActionParameters implements TypedNodeParameters {

    // ========== Основні параметри ==========

    @Builder.Default
    private TelegramResource resource = TelegramResource.MESSAGE;

    @Builder.Default
    private TelegramOperation operation = TelegramOperation.SEND;

    /**
     * Chat ID - обов'язковий параметр
     * Може бути: number, @username, або {{variable}}
     */
    private String chatId;

    // ========== Загальні параметри ==========

    /**
     * Відключити звукові сповіщення
     */
    @Builder.Default
    private Boolean disableNotification = false;

    /**
     * Захистити контент від пересилання та збереження
     */
    @Builder.Default
    private Boolean protectContent = false;

    /**
     * ID повідомлення для відповіді
     */
    private String replyToMessageId;

    /**
     * Дозволити відправку без відповіді якщо вказане повідомлення не знайдено
     */
    @Builder.Default
    private Boolean allowSendingWithoutReply = false;

    // ========== Message Parameters ==========

    /**
     * Текст повідомлення (для message, photo caption, etc.)
     */
    private String text;

    /**
     * Parse mode для форматування
     */
    @Builder.Default
    private TelegramParseMode parseMode = TelegramParseMode.NONE;

    /**
     * Відключити preview посилань
     */
    @Builder.Default
    private Boolean disableWebPagePreview = false;

    /**
     * Entities для форматування (альтернатива parseMode)
     */
    private List<Map<String, Object>> entities;

    // ========== Media Parameters (Photo, Video, Document, etc.) ==========

    /**
     * Тип вкладення (fileId, url, binary)
     */
    @Builder.Default
    private TelegramAttachmentType attachmentType = TelegramAttachmentType.URL;

    /**
     * URL фото
     */
    private String photoUrl;

    /**
     * File ID фото
     */
    private String photoFileId;

    /**
     * URL відео
     */
    private String videoUrl;

    /**
     * File ID відео
     */
    private String videoFileId;

    /**
     * URL документа
     */
    private String documentUrl;

    /**
     * File ID документа
     */
    private String documentFileId;

    /**
     * URL аудіо
     */
    private String audioUrl;

    /**
     * File ID аудіо
     */
    private String audioFileId;

    /**
     * URL голосового повідомлення
     */
    private String voiceUrl;

    /**
     * File ID голосового повідомлення
     */
    private String voiceFileId;

    /**
     * Caption для медіа (підпис)
     */
    private String caption;

    /**
     * Parse mode для caption
     */
    private TelegramParseMode captionParseMode;

    /**
     * Entities для caption
     */
    private List<Map<String, Object>> captionEntities;

    /**
     * Показати спойлер для медіа
     */
    @Builder.Default
    private Boolean hasSpoiler = false;

    // ========== Video Specific ==========

    /**
     * Тривалість відео в секундах
     */
    private Integer duration;

    /**
     * Ширина відео
     */
    private Integer width;

    /**
     * Висота відео
     */
    private Integer height;

    /**
     * URL thumbnail для відео
     */
    private String thumbnailUrl;

    /**
     * Чи підтримує streaming
     */
    @Builder.Default
    private Boolean supportsStreaming = false;

    // ========== Audio Specific ==========

    /**
     * Performer аудіо
     */
    private String performer;

    /**
     * Назва аудіо
     */
    private String title;

    // ========== Document Specific ==========

    /**
     * Відключити автоматичне визначення типу контенту
     */
    @Builder.Default
    private Boolean disableContentTypeDetection = false;

    // ========== Location Parameters ==========

    /**
     * Широта
     */
    private Double latitude;

    /**
     * Довгота
     */
    private Double longitude;

    /**
     * Horizontal accuracy в метрах (0-1500)
     */
    private Double horizontalAccuracy;

    /**
     * Період в секундах для live location (60-86400)
     */
    private Integer livePeriod;

    /**
     * Напрямок руху (1-360 градусів)
     */
    private Integer heading;

    /**
     * Максимальна відстань для оповіщення (1-100000 метрів)
     */
    private Integer proximityAlertRadius;

    // ========== Contact Parameters ==========

    /**
     * Номер телефону
     */
    private String phoneNumber;

    /**
     * Ім'я контакту
     */
    private String firstName;

    /**
     * Прізвище контакту
     */
    private String lastName;

    /**
     * vCard контакту
     */
    private String vcard;

    // ========== Venue Parameters ==========

    /**
     * Назва місця
     */
    private String venueName;

    /**
     * Адреса місця
     */
    private String address;

    /**
     * Foursquare ID
     */
    private String foursquareId;

    /**
     * Foursquare type
     */
    private String foursquareType;

    /**
     * Google Places ID
     */
    private String googlePlaceId;

    /**
     * Google Places type
     */
    private String googlePlaceType;

    // ========== Poll Parameters ==========

    /**
     * Питання опитування
     */
    private String question;

    /**
     * Варіанти відповідей
     */
    private List<String> pollOptions;

    /**
     * Чи анонімний опит
     */
    @Builder.Default
    private Boolean isAnonymous = true;

    /**
     * Тип опитування (quiz/regular)
     */
    @Builder.Default
    private String pollType = "regular";

    /**
     * Дозволити множинний вибір
     */
    @Builder.Default
    private Boolean allowsMultipleAnswers = false;

    /**
     * Правильний варіант для quiz (0-based index)
     */
    private Integer correctOptionId;

    /**
     * Пояснення для quiz
     */
    private String explanation;

    /**
     * Parse mode для пояснення
     */
    private TelegramParseMode explanationParseMode;

    /**
     * Час в секундах до закриття (5-600)
     */
    private Integer openPeriod;

    /**
     * Unix timestamp коли опитування закриється
     */
    private Long closeDate;

    /**
     * Чи закрито опитування
     */
    @Builder.Default
    private Boolean isClosed = false;

    // ========== Edit Parameters ==========

    /**
     * ID повідомлення для редагування
     */
    private String messageId;

    /**
     * Inline message ID для редагування
     */
    private String inlineMessageId;

    // ========== Forward Parameters ==========

    /**
     * From chat ID для пересилання
     */
    private String fromChatId;

    /**
     * Відключити link preview при пересиланні
     */
    @Builder.Default
    private Boolean disableLinkPreview = false;

    // ========== Pin/Unpin Parameters ==========

    /**
     * Відключити сповіщення при закріпленні
     */
    @Builder.Default
    private Boolean disablePinNotification = false;

    // ========== Reply Keyboard ==========

    /**
     * Inline клавіатура або reply markup
     */
    private Map<String, Object> replyMarkup;

    /**
     * Buttons для швидкого створення клавіатури
     */
    private List<List<Map<String, Object>>> buttons;

    // ========== Additional Options ==========

    /**
     * Чи продовжувати при помилці
     */
    @Builder.Default
    private Boolean continueOnFail = false;

    /**
     * Затримка між запитами (мс)
     */
    @Builder.Default
    private Integer delayBetweenRequests = 0;

    /**
     * Максимальна кількість повторів
     */
    @Builder.Default
    private Integer retryAttempts = 0;

    /**
     * Timeout для запиту (секунди)
     */
    @Builder.Default
    private Integer requestTimeout = 30;

    // ========== Methods ==========

    @Override
    public void validate() throws IllegalArgumentException {
        // Chat ID завжди обов'язковий (окрім inline messages)
        if (chatId == null || chatId.trim().isEmpty()) {
            if (inlineMessageId == null || inlineMessageId.trim().isEmpty()) {
                throw new IllegalArgumentException("chatId is required (or inlineMessageId for inline messages)");
            }
        }

        // Валідація залежно від resource
        switch (resource) {
            case MESSAGE -> validateMessageResource();
            case PHOTO -> validatePhotoResource();
            case VIDEO -> validateVideoResource();
            case DOCUMENT -> validateDocumentResource();
            case AUDIO -> validateAudioResource();
            case VOICE -> validateVoiceResource();
            case LOCATION -> validateLocationResource();
            case CONTACT -> validateContactResource();
            case VENUE -> validateVenueResource();
            case POLL -> validatePollResource();
            default -> {}
            // Інші типи поки що не валідуються
        }

        // Валідація залежно від operation
        if (operation == TelegramOperation.EDIT && messageId == null && inlineMessageId == null) {
            throw new IllegalArgumentException("messageId or inlineMessageId is required for edit operation");
        }

        if (operation == TelegramOperation.FORWARD) {
            if (fromChatId == null || fromChatId.trim().isEmpty()) {
                throw new IllegalArgumentException("fromChatId is required for forward operation");
            }
            if (messageId == null || messageId.trim().isEmpty()) {
                throw new IllegalArgumentException("messageId is required for forward operation");
            }
        }
    }

    private void validateMessageResource() {
        if (operation == TelegramOperation.SEND) {
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("text is required for message resource");
            }
        }
    }

    private void validatePhotoResource() {
        if (operation == TelegramOperation.SEND) {
            if (attachmentType == TelegramAttachmentType.URL && (photoUrl == null || photoUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("photoUrl is required when attachmentType is URL");
            }
            if (attachmentType == TelegramAttachmentType.FILE_ID && (photoFileId == null || photoFileId.trim().isEmpty())) {
                throw new IllegalArgumentException("photoFileId is required when attachmentType is fileId");
            }
        }
    }

    private void validateVideoResource() {
        if (operation == TelegramOperation.SEND) {
            if (attachmentType == TelegramAttachmentType.URL && (videoUrl == null || videoUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("videoUrl is required when attachmentType is URL");
            }
            if (attachmentType == TelegramAttachmentType.FILE_ID && (videoFileId == null || videoFileId.trim().isEmpty())) {
                throw new IllegalArgumentException("videoFileId is required when attachmentType is fileId");
            }
        }
    }

    private void validateDocumentResource() {
        if (operation == TelegramOperation.SEND) {
            if (attachmentType == TelegramAttachmentType.URL && (documentUrl == null || documentUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("documentUrl is required when attachmentType is URL");
            }
            if (attachmentType == TelegramAttachmentType.FILE_ID && (documentFileId == null || documentFileId.trim().isEmpty())) {
                throw new IllegalArgumentException("documentFileId is required when attachmentType is fileId");
            }
        }
    }

    private void validateAudioResource() {
        if (operation == TelegramOperation.SEND) {
            if (attachmentType == TelegramAttachmentType.URL && (audioUrl == null || audioUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("audioUrl is required when attachmentType is URL");
            }
            if (attachmentType == TelegramAttachmentType.FILE_ID && (audioFileId == null || audioFileId.trim().isEmpty())) {
                throw new IllegalArgumentException("audioFileId is required when attachmentType is fileId");
            }
        }
    }

    private void validateVoiceResource() {
        if (operation == TelegramOperation.SEND) {
            if (attachmentType == TelegramAttachmentType.URL && (voiceUrl == null || voiceUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("voiceUrl is required when attachmentType is URL");
            }
            if (attachmentType == TelegramAttachmentType.FILE_ID && (voiceFileId == null || voiceFileId.trim().isEmpty())) {
                throw new IllegalArgumentException("voiceFileId is required when attachmentType is fileId");
            }
        }
    }

    private void validateLocationResource() {
        if (operation == TelegramOperation.SEND) {
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("latitude and longitude are required for location resource");
            }
            if (latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException("latitude must be between -90 and 90");
            }
            if (longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("longitude must be between -180 and 180");
            }
        }
    }

    private void validateContactResource() {
        if (operation == TelegramOperation.SEND) {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("phoneNumber is required for contact resource");
            }
            if (firstName == null || firstName.trim().isEmpty()) {
                throw new IllegalArgumentException("firstName is required for contact resource");
            }
        }
    }

    private void validateVenueResource() {
        if (operation == TelegramOperation.SEND) {
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("latitude and longitude are required for venue resource");
            }
            if (venueName == null || venueName.trim().isEmpty()) {
                throw new IllegalArgumentException("venueName is required for venue resource");
            }
            if (address == null || address.trim().isEmpty()) {
                throw new IllegalArgumentException("address is required for venue resource");
            }
        }
    }

    private void validatePollResource() {
        if (operation == TelegramOperation.SEND) {
            if (question == null || question.trim().isEmpty()) {
                throw new IllegalArgumentException("question is required for poll resource");
            }
            if (pollOptions == null || pollOptions.size() < 2) {
                throw new IllegalArgumentException("at least 2 poll options are required");
            }
            if (pollOptions.size() > 10) {
                throw new IllegalArgumentException("maximum 10 poll options allowed");
            }
        }
    }

    @Override
    public NodeDiscriminator getParameterType() {
        return NodeDiscriminator.TELEGRAM_BOT_ACTION;
    }

    // ========== Helper Methods ==========

    public String getEffectiveParseMode() {
        return parseMode != null ? parseMode.getTelegramValue() : null;
    }

    public String getEffectiveCaptionParseMode() {
        return captionParseMode != null ? captionParseMode.getTelegramValue() : null;
    }

    public boolean isMessageResource() {
        return resource == TelegramResource.MESSAGE;
    }

    public boolean isPhotoResource() {
        return resource == TelegramResource.PHOTO;
    }

    public boolean isVideoResource() {
        return resource == TelegramResource.VIDEO;
    }

    public boolean isDocumentResource() {
        return resource == TelegramResource.DOCUMENT;
    }

    public boolean isLocationResource() {
        return resource == TelegramResource.LOCATION;
    }

    public boolean isContactResource() {
        return resource == TelegramResource.CONTACT;
    }

    public boolean isVenueResource() {
        return resource == TelegramResource.VENUE;
    }

    public boolean isPollResource() {
        return resource == TelegramResource.POLL;
    }

    public boolean isEditOperation() {
        return operation == TelegramOperation.EDIT;
    }

    public boolean isSendOperation() {
        return operation == TelegramOperation.SEND;
    }

    public boolean isDeleteOperation() {
        return operation == TelegramOperation.DELETE;
    }

    public boolean isForwardOperation() {
        return operation == TelegramOperation.FORWARD;
    }

    public boolean hasReplyMarkup() {
        return replyMarkup != null && !replyMarkup.isEmpty();
    }

    public boolean hasButtons() {
        return buttons != null && !buttons.isEmpty();
    }
}