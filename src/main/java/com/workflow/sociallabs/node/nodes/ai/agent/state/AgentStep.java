package com.workflow.sociallabs.node.nodes.ai.agent.state;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentResponse;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.TokenUsage;
import com.workflow.sociallabs.node.nodes.ai.agent.tool.ToolExecutionResult;
import lombok.NonNull;

import java.time.Instant;

public sealed interface AgentStep permits AgentStep.LlmStep, AgentStep.ToolStep, AgentStep.ErrorStep {

    Instant timestamp();

    record LlmStep(
            @NonNull Instant timestamp,
            @NonNull TokenUsage usage,
            @NonNull AgentResponse.StopReason stopReason
    ) implements AgentStep {
    }

    record ToolStep(
            @NonNull Instant timestamp,
            @NonNull ToolExecutionResult result
    ) implements AgentStep {
    }

    record ErrorStep(
            @NonNull Instant timestamp,
            @NonNull String message,
            boolean fatal
    ) implements AgentStep {
    }
}
