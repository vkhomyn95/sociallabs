package com.workflow.sociallabs.node.nodes.telegram.client.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.*;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Типізовані параметри для Telegram Client Action
 * Використовує TDLight для роботи через Telegram Client API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName(NodeDiscriminator.Values.TELEGRAM_CLIENT_ACTION)
public class TelegramClientActionParameters implements TypedNodeParameters {

    // ========== Основні параметри ==========

    @Builder.Default
    private TelegramClientResource resource = TelegramClientResource.MESSAGE;

    @Builder.Default
    private TelegramClientOperation operation = TelegramClientOperation.SEND;

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
    private Long replyToMessageId;

    /**
     * ID теми (topic) в супергрупах з темами
     */
    private Long messageThreadId;

    /**
     * Scheduled time для відкладеної відправки (Unix timestamp)
     */
    private Integer schedulingState;

    // ========== Message Parameters ==========

    /**
     * Текст повідомлення
     */
    private String text;

    /**
     * Parse mode для форматування
     */
    @Builder.Default
    private TelegramClientParseMode parseMode = TelegramClientParseMode.TEXT;

    /**
     * Відключити preview посилань
     */
    @Builder.Default
    private Boolean disableWebPagePreview = false;

    /**
     * Очистити draft після відправки
     */
    @Builder.Default
    private Boolean clearDraft = true;

    // ========== Media Parameters ==========

    /**
     * Тип вкладення (local, remote, fileId)
     */
    @Builder.Default
    private TelegramClientAttachmentType attachmentType = TelegramClientAttachmentType.REMOTE;

    /**
     * Шлях до локального файлу
     */
    private String localFilePath;

    /**
     * URL віддаленого файлу
     */
    private String remoteFileUrl;

    /**
     * File ID в Telegram
     */
    private Integer fileId;

    /**
     * Caption для медіа
     */
    private String caption;

    /**
     * Parse mode для caption
     */
    private TelegramClientParseMode captionParseMode;

    /**
     * TTL (Time To Live) для медіа в секундах (самознищення)
     */
    private Integer ttl;

    /**
     * Показати спойлер для медіа
     */
    @Builder.Default
    private Boolean hasSpoiler = false;

    // ========== Photo Specific ==========

    /**
     * Compression quality для фото (1-100)
     */
    @Builder.Default
    private Integer photoCompressionQuality = 85;

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
     * Чи підтримує streaming
     */
    @Builder.Default
    private Boolean supportsStreaming = false;

    /**
     * Шлях до thumbnail
     */
    private String thumbnailPath;

    // ========== Audio Specific ==========

    /**
     * Performer аудіо
     */
    private String performer;

    /**
     * Назва аудіо
     */
    private String title;

    /**
     * Album name
     */
    private String albumName;

    // ========== Document Specific ==========

    /**
     * MIME type документа
     */
    private String mimeType;

    /**
     * Відключити автоматичне визначення типу
     */
    @Builder.Default
    private Boolean disableContentTypeDetection = false;

    // ========== Voice/Video Note Specific ==========

    /**
     * Waveform для голосового повідомлення
     */
    private byte[] waveform;

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

    /**
     * User ID контакту (якщо це Telegram користувач)
     */
    private Long userId;

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
     * Provider venue
     */
    private String provider;

    /**
     * Venue ID
     */
    private String venueId;

    /**
     * Venue type
     */
    private String venueType;

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
     * Тип опитування (regular/quiz)
     */
    @Builder.Default
    private TelegramClientPollType pollType = TelegramClientPollType.REGULAR;

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
    private TelegramClientParseMode explanationParseMode;

    /**
     * Час в секундах до закриття (5-600)
     */
    private Integer openPeriod;

    /**
     * Чи закрито опитування
     */
    @Builder.Default
    private Boolean isClosed = false;

    // ========== Sticker Parameters ==========

    /**
     * Emoji для стікера
     */
    private String emoji;

    // ========== Animation Parameters ==========

    /**
     * Додати до нещодавніх
     */
    @Builder.Default
    private Boolean addToRecent = true;

    // ========== Dice Parameters ==========

    /**
     * Emoji для dice (🎲, 🎯, 🏀, ⚽, 🎰, 🎳)
     */
    @Builder.Default
    private String diceEmoji = "🎲";

    // ========== Edit Parameters ==========

    /**
     * ID повідомлення для редагування
     */
    private Long messageId;

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
     * IDs повідомлень для пересилання
     */
    private List<Long> messageIds;

    /**
     * Надіслати копію без автора
     */
    @Builder.Default
    private Boolean sendCopy = false;

    /**
     * Видалити caption при копіюванні
     */
    @Builder.Default
    private Boolean removeCaption = false;

    /**
     * Надіслати як альбом (групою)
     */
    @Builder.Default
    private Boolean sendAsAlbum = true;

    // ========== Delete Parameters ==========

    /**
     * Видалити для всіх
     */
    @Builder.Default
    private Boolean revoke = true;

    // ========== Read Parameters ==========

    /**
     * Mark as read
     */
    @Builder.Default
    private Boolean markAsRead = true;

    // ========== Typing Parameters ==========

    /**
     * Action typing (typing, upload_photo, record_video, etc.)
     */
    @Builder.Default
    private TelegramClientChatAction chatAction = TelegramClientChatAction.TYPING;

    // ========== Reply Markup ==========

    /**
     * Inline клавіатура або reply markup
     */
    private Map<String, Object> replyMarkup;

    /**
     * Buttons для швидкого створення клавіатури
     */
    private List<List<Map<String, Object>>> buttons;

    /**
     * Reply markup type (inline, keyboard, remove, force_reply)
     */
    @Builder.Default
    private TelegramClientReplyMarkupType replyMarkupType = TelegramClientReplyMarkupType.INLINE;

    /**
     * One-time keyboard
     */
    @Builder.Default
    private Boolean oneTimeKeyboard = false;

    /**
     * Resize keyboard
     */
    @Builder.Default
    private Boolean resizeKeyboard = true;

    /**
     * Selective (показувати тільки певним користувачам)
     */
    @Builder.Default
    private Boolean selective = false;

    /**
     * Placeholder для input field
     */
    private String inputFieldPlaceholder;

    // ========== Chat Member Parameters ==========

    /**
     * User ID члена чату
     */
    private Long memberUserId;

    /**
     * Status члена чату (administrator, member, restricted, left, banned)
     */
    private TelegramClientChatMemberStatus memberStatus;

    /**
     * Custom title для адміністратора
     */
    private String customTitle;

    /**
     * Права адміністратора
     */
    private Map<String, Boolean> administratorRights;

    /**
     * Обмеження для restricted користувача
     */
    private Map<String, Boolean> restrictedRights;

    /**
     * Until date для обмежень (Unix timestamp)
     */
    private Integer untilDate;

    // ========== Chat Parameters ==========

    /**
     * Назва чату
     */
    private String chatTitle;

    /**
     * Опис чату
     */
    private String chatDescription;

    /**
     * Username чату
     */
    private String chatUsername;

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
        // Chat ID завжди обов'язковий (окрім inline messages та деяких операцій)
        if (chatId == null || chatId.trim().isEmpty()) {
            if (inlineMessageId == null || inlineMessageId.trim().isEmpty()) {
                if (!isSpecialOperation()) {
                    throw new IllegalArgumentException("chatId is required");
                }
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
            case VIDEO_NOTE -> validateVideoNoteResource();
            case LOCATION -> validateLocationResource();
            case CONTACT -> validateContactResource();
            case VENUE -> validateVenueResource();
            case POLL -> validatePollResource();
            case STICKER -> validateStickerResource();
            case ANIMATION -> validateAnimationResource();
            case DICE -> validateDiceResource();
            default -> {}
        }

        // Валідація залежно від operation
        validateOperation();
    }

    private boolean isSpecialOperation() {
        return operation == TelegramClientOperation.GET_ME ||
                operation == TelegramClientOperation.GET_CHATS ||
                operation == TelegramClientOperation.SEARCH_MESSAGES;
    }

    private void validateOperation() {
        switch (operation) {
            case EDIT -> {
                if (messageId == null && inlineMessageId == null) {
                    throw new IllegalArgumentException("messageId or inlineMessageId is required for edit operation");
                }
            }
            case FORWARD -> {
                if (fromChatId == null || fromChatId.trim().isEmpty()) {
                    throw new IllegalArgumentException("fromChatId is required for forward operation");
                }
                if (messageIds == null || messageIds.isEmpty()) {
                    throw new IllegalArgumentException("messageIds is required for forward operation");
                }
            }
            case DELETE -> {
                if (messageId == null) {
                    throw new IllegalArgumentException("messageId is required for delete operation");
                }
            }
            case ADD_MEMBER, REMOVE_MEMBER, PROMOTE_MEMBER, RESTRICT_MEMBER, BAN_MEMBER, UNBAN_MEMBER -> {
                if (memberUserId == null) {
                    throw new IllegalArgumentException("memberUserId is required for member operations");
                }
            }
        }
    }

    private void validateMessageResource() {
        if (operation == TelegramClientOperation.SEND) {
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("text is required for message resource");
            }
        }
    }

    private void validatePhotoResource() {
        if (operation == TelegramClientOperation.SEND) {
            validateMediaSource("photo");
        }
    }

    private void validateVideoResource() {
        if (operation == TelegramClientOperation.SEND) {
            validateMediaSource("video");
        }
    }

    private void validateDocumentResource() {
        if (operation == TelegramClientOperation.SEND) {
            validateMediaSource("document");
        }
    }

    private void validateAudioResource() {
        if (operation == TelegramClientOperation.SEND) {
            validateMediaSource("audio");
        }
    }

    private void validateVoiceResource() {
        if (operation == TelegramClientOperation.SEND) {
            validateMediaSource("voice");
        }
    }

    private void validateVideoNoteResource() {
        if (operation == TelegramClientOperation.SEND) {
            validateMediaSource("video note");
        }
    }

    private void validateStickerResource() {
        if (operation == TelegramClientOperation.SEND) {
            validateMediaSource("sticker");
        }
    }

    private void validateAnimationResource() {
        if (operation == TelegramClientOperation.SEND) {
            validateMediaSource("animation");
        }
    }

    private void validateMediaSource(String mediaType) {
        switch (attachmentType) {
            case LOCAL -> {
                if (localFilePath == null || localFilePath.trim().isEmpty()) {
                    throw new IllegalArgumentException("localFilePath is required when attachmentType is LOCAL for " + mediaType);
                }
            }
            case REMOTE -> {
                if (remoteFileUrl == null || remoteFileUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException("remoteFileUrl is required when attachmentType is REMOTE for " + mediaType);
                }
            }
            case FILE_ID -> {
                if (fileId == null) {
                    throw new IllegalArgumentException("fileId is required when attachmentType is FILE_ID for " + mediaType);
                }
            }
        }
    }

    private void validateLocationResource() {
        if (operation == TelegramClientOperation.SEND) {
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
        if (operation == TelegramClientOperation.SEND) {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("phoneNumber is required for contact resource");
            }
            if (firstName == null || firstName.trim().isEmpty()) {
                throw new IllegalArgumentException("firstName is required for contact resource");
            }
        }
    }

    private void validateVenueResource() {
        if (operation == TelegramClientOperation.SEND) {
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
        if (operation == TelegramClientOperation.SEND) {
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

    private void validateDiceResource() {
        if (operation == TelegramClientOperation.SEND) {
            if (diceEmoji == null || diceEmoji.trim().isEmpty()) {
                throw new IllegalArgumentException("diceEmoji is required for dice resource");
            }
            List<String> validEmojis = List.of("🎲", "🎯", "🏀", "⚽", "🎰", "🎳");
            if (!validEmojis.contains(diceEmoji)) {
                throw new IllegalArgumentException("Invalid dice emoji. Must be one of: 🎲, 🎯, 🏀, ⚽, 🎰, 🎳");
            }
        }
    }

    @Override
    public NodeDiscriminator getParameterType() {
        return NodeDiscriminator.TELEGRAM_CLIENT_ACTION;
    }

    // ========== Helper Methods ==========

    public boolean hasReplyMarkup() {
        return replyMarkup != null && !replyMarkup.isEmpty();
    }

    public boolean hasButtons() {
        return buttons != null && !buttons.isEmpty();
    }

    public boolean isMessageResource() {
        return resource == TelegramClientResource.MESSAGE;
    }

    public boolean isMediaResource() {
        return resource == TelegramClientResource.PHOTO ||
                resource == TelegramClientResource.VIDEO ||
                resource == TelegramClientResource.DOCUMENT ||
                resource == TelegramClientResource.AUDIO ||
                resource == TelegramClientResource.VOICE ||
                resource == TelegramClientResource.VIDEO_NOTE ||
                resource == TelegramClientResource.STICKER ||
                resource == TelegramClientResource.ANIMATION;
    }

    public boolean isSendOperation() {
        return operation == TelegramClientOperation.SEND;
    }

    public boolean isEditOperation() {
        return operation == TelegramClientOperation.EDIT;
    }

    public boolean isDeleteOperation() {
        return operation == TelegramClientOperation.DELETE;
    }

    public boolean isForwardOperation() {
        return operation == TelegramClientOperation.FORWARD;
    }
}
