package com.workflow.sociallabs.node.nodes.ai.agent.tool.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.workflow.sociallabs.node.nodes.ai.agent.exception.ToolExecutionException;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.AgentTool;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolContext;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolInput;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolOutput;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeExecutor;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.core.WorkflowItem;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;

import java.util.Map;
import java.util.Objects;

/**
 * Базовий адаптер який обгортає існуючий NodeExecutor в AgentTool.
 * Код executor-а НЕ дублюється — тільки маппинг параметрів.
 * <p>
 * I — спрощений ToolInput (те що LLM реально знає)
 * O — ToolOutput
 * P — TypedNodeParameters (повний параметр ноди)
 */
public abstract class NodeExecutorToolAdapter
        <I extends ToolInput,
        O extends ToolOutput,
        P extends TypedNodeParameters>
        implements AgentTool<I, O> {

    private final NodeExecutor executor;

    protected NodeExecutorToolAdapter(NodeExecutor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public final O execute(I input, ToolContext context) throws ToolExecutionException {
        // 1. Map спрощений ToolInput → повний TypedNodeParameters
        P nodeParams = mapInputToNodeParameters(input, context);

        // 2. Валідація через існуючий механізм ноди
        try {
            nodeParams.validate();
        } catch (IllegalArgumentException e) {
            throw new ToolExecutionException("VALIDATION_FAILED", e.getMessage(), false);
        }

        // 3. Будуємо ExecutionContext для executor-а
        ExecutionContext execContext = ExecutionContext.builder()
                .nodeId("tool-" + getName() + "-" + context.getExecutionCorrelationId())
                .workflowId(context.getWorkflowId())
                .inputItems(WorkflowItem.single(inputToJson(input)))
                .parameters(nodeParams)
                .credentials(context.getSharedCredentials())
                .build();

        // 4. Делегуємо в існуючий executor — НУЛЬ дублювання
        NodeResult result = executor.execute(execContext);

        // 5. Маппимо NodeResult → O
        if (!result.isSuccess()) {
            throw new ToolExecutionException("NODE_EXECUTION_FAILED", result.getError(), true);
        }

        return mapNodeResultToOutput(result, input);
    }

    /**
     * Маппить спрощений ToolInput → повний TypedNodeParameters ноди.
     * Тут заповнюємо defaults із параметрів ноди що вже налаштовані в UI.
     */
    protected abstract P mapInputToNodeParameters(I input, ToolContext context);

    /**
     * Маппить NodeResult → строго-типізований O
     */
    protected abstract O mapNodeResultToOutput(NodeResult result, I input);

    /**
     * Конвертуємо ToolInput → Map для WorkflowItem
     * Default: через ObjectMapper, можна перевизначити
     */
    protected Map<String, Object> inputToJson(I input) {
        // Jackson серіалізація record → Map автоматично
        return MAPPER.convertValue(input, new TypeReference<>() {});
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
}