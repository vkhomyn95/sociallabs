package com.workflow.sociallabs.node.nodes.ai.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.ai.agent.AgentLimits;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.ModelId;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.MemoryConfig;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import jakarta.annotation.Nullable;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName(NodeDiscriminator.Values.AI_AGENT)
public class AiAgentNodeParameters implements TypedNodeParameters {

    @NonNull
    private ModelId modelId;          // "claude-sonnet-4-20250514"

    @NonNull
    @Builder.Default
    private MemoryConfig memory = MemoryConfig.disabled();

    private String systemPrompt = "You are a helpful assistant.";

    /**
     * З якого поля input item брати user message.
     * Якщо null — серіалізуємо весь item.json як prompt.
     */
    @Nullable
    private String inputField;

    @Builder.Default
    private List<String> toolNames = List.of();  // ["http_request", "get_db_records"]
    @NonNull
    @Builder.Default
    private AgentLimits limits = AgentLimits.builder().build(); // maxIterations, timeoutMs
    @NonNull
    @Builder.Default
    private String outputField = "answer";      // куди пишемо фінальну відповідь

    @NonNull
    @Builder.Default
    private OutputMode outputMode = OutputMode.ANSWER_ONLY;

    @Builder.Default
    private boolean continueOnFail = false; // В output item буде поле "error" з причиною.

    @Override
    public void validate() throws IllegalArgumentException {
        validateSystemPrompt();
        validateMemory();
    }

    @Override
    public NodeDiscriminator getParameterType() {
        return NodeDiscriminator.AI_AGENT;
    }

    public enum OutputMode {
        ANSWER_ONLY, // ANSWER_ONLY  — тільки фінальна відповідь (default)
        WITH_STEPS, // WITH_STEPS   — відповідь + intermediate steps для debugging
        STRUCTURED // STRUCTURED   — parse JSON з відповіді LLM в окремі поля
    }

    private void validateSystemPrompt() {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt must not be blank");
        }
        if (systemPrompt.length() > 10_000) {
            throw new IllegalArgumentException(
                    "systemPrompt must not exceed 10000 characters, got: " + systemPrompt.length());
        }
    }

    private void validateMemory() {
        if (memory == null) {
            throw new IllegalArgumentException("memory config must not be null");
        }
        if (memory.isEnabled()) {
            if (memory.getSessionKeyStrategy()
                    == MemoryConfig.SessionKeyStrategy.FROM_ITEM) {
                if (memory.getSessionKeyField() == null
                        || memory.getSessionKeyField().isBlank()) {
                    throw new IllegalArgumentException(
                            "memory.sessionKeyField is required when strategy is FROM_ITEM");
                }
            }
        }
    }

    public boolean hasTools() {
        return toolNames != null && !toolNames.isEmpty();
    }

    public boolean hasMemory() {
        return memory != null && memory.isEnabled();
    }

    public boolean isStructuredOutput() {
        return outputMode == OutputMode.STRUCTURED;
    }

    public boolean isWithSteps() {
        return outputMode == OutputMode.WITH_STEPS;
    }
}

