package com.workflow.sociallabs.node.nodes.telegram.client.enums;

import it.tdlight.jni.TdApi;

public enum TelegramClientChatAction {
    TYPING,
    UPLOAD_PHOTO,
    RECORD_VIDEO,
    UPLOAD_VIDEO,
    RECORD_VOICE,
    UPLOAD_VOICE,
    UPLOAD_DOCUMENT,
    CHOOSE_STICKER,
    FIND_LOCATION,
    RECORD_VIDEO_NOTE,
    UPLOAD_VIDEO_NOTE,
    CANCEL;

    public TdApi.ChatAction toTdApi() {
        return switch (this) {
            case TYPING -> new TdApi.ChatActionTyping();
            case UPLOAD_PHOTO -> new TdApi.ChatActionUploadingPhoto();
            case RECORD_VIDEO -> new TdApi.ChatActionRecordingVideo();
            case UPLOAD_VIDEO -> new TdApi.ChatActionUploadingVideo();
            case RECORD_VOICE -> new TdApi.ChatActionRecordingVoiceNote();
            case UPLOAD_VOICE -> new TdApi.ChatActionUploadingVoiceNote();
            case UPLOAD_DOCUMENT -> new TdApi.ChatActionUploadingDocument();
            case CHOOSE_STICKER -> new TdApi.ChatActionChoosingSticker();
            case FIND_LOCATION -> new TdApi.ChatActionChoosingLocation();
            case RECORD_VIDEO_NOTE -> new TdApi.ChatActionRecordingVideoNote();
            case UPLOAD_VIDEO_NOTE -> new TdApi.ChatActionUploadingVideoNote();
            case CANCEL -> new TdApi.ChatActionCancel();
        };
    }
}
