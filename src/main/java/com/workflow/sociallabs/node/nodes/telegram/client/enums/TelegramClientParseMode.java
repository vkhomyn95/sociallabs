package com.workflow.sociallabs.node.nodes.telegram.client.enums;

import it.tdlight.jni.TdApi;

public enum TelegramClientParseMode {
    TEXT,
    MARKDOWN,
    HTML;

    public TdApi.TextParseMode toTdApi() {
        return switch (this) {
            case MARKDOWN -> new TdApi.TextParseModeMarkdown(1);
            case HTML -> new TdApi.TextParseModeHTML();
            case TEXT -> null;
        };
    }
}
