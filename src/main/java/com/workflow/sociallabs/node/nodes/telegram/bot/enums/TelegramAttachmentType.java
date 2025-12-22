package com.workflow.sociallabs.node.nodes.telegram.bot.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Тип вкладення
 */
@Getter
@RequiredArgsConstructor
public enum TelegramAttachmentType {
    FILE_ID("fileId"),
    URL("url"),
    BINARY("binary");

    @JsonValue
    private final String value;
}
