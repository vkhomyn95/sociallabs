package com.workflow.sociallabs.node.nodes.ai.agent.state;

import com.workflow.sociallabs.node.nodes.ai.agent.AgentLimits;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.ModelId;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.TokenUsage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class AgentState {

    @NonNull ModelId modelId;
    @NonNull String systemPrompt;
    @NonNull List<AgentMessage> messages;        // незмінний snapshot
    @NonNull List<AgentStep> steps;
    @NonNull AgentLimits limits;
    @Builder.Default
    int iterations = 0;
    @Builder.Default
    int toolCallsUsed = 0;
    @Builder.Default
    int consecutiveErrors = 0;
    @Builder.Default
    long totalTokensUsed = 0;

    /**
     * Immutable update — повертає новий стан
     */
    public AgentState withNewMessage(AgentMessage message) {
        List<AgentMessage> updated = new ArrayList<>(messages);
        updated.add(message);
        return toBuilder()
                .messages(Collections.unmodifiableList(updated))
                .iterations(iterations + 1)
                .build();
    }

    public AgentState withToolResult(AgentMessage.ToolResultMessage result, AgentStep step) {
        List<AgentMessage> msgs = new ArrayList<>(messages);
        List<AgentStep> steps = new ArrayList<>(this.steps);
        msgs.add(result);
        steps.add(step);
        return toBuilder()
                .messages(Collections.unmodifiableList(msgs))
                .steps(Collections.unmodifiableList(steps))
                .toolCallsUsed(toolCallsUsed + 1)
                .consecutiveErrors(result.result().isSuccess() ? 0 : consecutiveErrors + 1)
                .build();
    }

    public AgentState withTokens(TokenUsage usage) {
        return toBuilder()
                .totalTokensUsed(totalTokensUsed + usage.total())
                .build();
    }

    public static AgentState initial(
            ModelId modelId, String systemPrompt,
            String userInput, AgentLimits limits
    ) {
        return AgentState.builder()
                .modelId(modelId)
                .systemPrompt(systemPrompt)
                .messages(List.of(new AgentMessage.UserMessage(userInput)))
                .steps(List.of())
                .limits(limits)
                .build();
    }

    public AgentState withPrependedHistory(List<AgentMessage> history) {
        List<AgentMessage> combined = new ArrayList<>(history);
        combined.addAll(messages);  // history + поточний user message
        return toBuilder()
                .messages(Collections.unmodifiableList(combined))
                .build();
    }
}
