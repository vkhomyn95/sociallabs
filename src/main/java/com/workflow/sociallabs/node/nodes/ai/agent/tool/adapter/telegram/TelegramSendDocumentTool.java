package com.workflow.sociallabs.node.nodes.ai.agent.tool.adapter.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolContext;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolInput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema;
import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientActionNodeExecutor;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientAttachmentType;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.STRING;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientOperation.SEND;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientResource.DOCUMENT;

@Component
public final class TelegramSendDocumentTool extends AbstractTelegramClientToolAdapter<TelegramSendDocumentTool.Input> {

    public TelegramSendDocumentTool(TelegramClientActionNodeExecutor executor) {
        super(executor);
    }

    @Override
    public String getName() {
        return "telegram_send_document";
    }

    @Override
    public String getDescription() {
        return "Send a file/document to a Telegram chat via URL. Use for PDFs, spreadsheets, or any file attachment.";
    }

    @Override
    public Class<Input> getInputType() {
        return Input.class;
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.builder()
                .name(getName()).description(getDescription())
                .parameter(param("chatId", STRING, "Chat ID or @username", true))
                .parameter(param("documentUrl", STRING, "Public URL of the document", true))
                .parameter(param("caption", STRING, "Optional caption", false))
                .parameter(param("fileName", STRING, "Optional custom file name", false))
                .required(List.of("chatId", "documentUrl"))
                .build();
    }

    @Override
    protected TelegramClientActionParameters mapInputToNodeParameters(
            Input input,
            ToolContext ctx
    ) {
        return baseBuilder(DOCUMENT, SEND, input.chatId())
                .attachmentType(TelegramClientAttachmentType.REMOTE)
                .remoteFileUrl(input.documentUrl())
                .caption(input.caption())
                .build();
    }

    public record Input(
            @JsonProperty(required = true) @NotBlank String chatId,
            @JsonProperty(required = true) @NotBlank String documentUrl,
            @JsonProperty @Nullable String caption,
            @JsonProperty @Nullable String fileName
    ) implements ToolInput {
    }
}
