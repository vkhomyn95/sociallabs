package com.workflow.sociallabs.node.nodes.ai.agent.tool.adapter.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolContext;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolInput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolOutput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema;
import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientActionNodeExecutor;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientAttachmentType;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema.ToolParameter.ParameterType.STRING;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientOperation.SEND;
import static com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientResource.PHOTO;

@Component
public final class TelegramSendPhotoTool extends AbstractTelegramClientToolAdapter<TelegramSendPhotoTool.Input> {

    public TelegramSendPhotoTool(TelegramClientActionNodeExecutor executor) {
        super(executor);
    }

    @Override
    public String getName() {
        return "telegram_send_photo";
    }

    @Override
    public String getDescription() {
        return "Send a photo to a Telegram chat via URL. Use when you need to share an image.";
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
                .parameter(param("photoUrl", STRING, "Public URL of the photo", true))
                .parameter(param("caption", STRING, "Optional caption text", false))
                .required(List.of("chatId", "photoUrl"))
                .build();
    }

    @Override
    protected TelegramClientActionParameters mapInputToNodeParameters(
            Input input,
            ToolContext ctx
    ) {
        return baseBuilder(PHOTO, SEND, input.chatId())
                .attachmentType(TelegramClientAttachmentType.REMOTE)
                .remoteFileUrl(input.photoUrl())
                .caption(input.caption())
                .build();
    }

    public record Input(
            @JsonProperty(required = true) @NotBlank String chatId,
            @JsonProperty(required = true) @NotBlank String photoUrl,
            @JsonProperty @Nullable String caption
    ) implements ToolInput {
    }
}
