package com.workflow.sociallabs.node.nodes.ai.agent.tool.adapter.telegram;

import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.core.WorkflowItem;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolInput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolOutput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolSchema;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.adapter.NodeExecutorToolAdapter;
import com.workflow.sociallabs.node.nodes.telegram.client.TelegramClientActionNodeExecutor;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientOperation;
import com.workflow.sociallabs.node.nodes.telegram.client.enums.TelegramClientResource;
import com.workflow.sociallabs.node.nodes.telegram.client.parameters.TelegramClientActionParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTelegramClientToolAdapter
        <I extends ToolInput> extends NodeExecutorToolAdapter<I, ToolOutput, TelegramClientActionParameters> {

    protected AbstractTelegramClientToolAdapter(TelegramClientActionNodeExecutor executor) {
        super(executor);
    }

    // ── Спільна логіка output для ВСІХ telegram tools ──────────

    @Override
    protected ToolOutput mapNodeResultToOutput(NodeResult result, I input) {
        List<WorkflowItem> items = result.getMainItems();
        Map<String, Object> data = items.isEmpty()
                ? Map.of("success", true)
                : new HashMap<>(items.get(0).json());
        return ToolOutput.Success.of(data);
    }

    // ── Спільні builder-методи для параметрів ──────────────────

    /**
     * Базовий builder вже налаштований з resource + operation
     */
    protected TelegramClientActionParameters.TelegramClientActionParametersBuilder baseBuilder(
            TelegramClientResource resource,
            TelegramClientOperation operation,
            String chatId
    ) {
        return TelegramClientActionParameters.builder()
                .resource(resource)
                .operation(operation)
                .chatId(chatId)
                .continueOnFail(false)
                .retryAttempts(1)
                .requestTimeout(30);
    }

    protected static ToolSchema.ToolParameter param(
            String name,
            ToolSchema.ToolParameter.ParameterType type,
            String description,
            boolean required
    ) {
        return ToolSchema.ToolParameter.builder()
                .name(name).type(type).description(description).build();
    }
}
