package com.workflow.sociallabs.node.nodes.ai.agent;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.TokenUsage;
import com.workflow.sociallabs.node.nodes.ai.agent.state.AgentStep;
import lombok.NonNull;

import java.util.List;

public sealed interface AgentResult
        permits AgentResult.Success, AgentResult.Failure {

    boolean isSuccess();

    List<AgentStep> steps();

    record Success(
            @NonNull String finalAnswer,
            @NonNull List<AgentStep> steps,
            @NonNull TokenUsage totalUsage
    ) implements AgentResult {
        public boolean isSuccess() {
            return true;
        }
    }

    record Failure(
            @NonNull String reason,
            @NonNull FailureType failureType,
            @NonNull List<AgentStep> steps
    ) implements AgentResult {
        public boolean isSuccess() {
            return false;
        }
    }

    enum FailureType {
        MAX_ITERATIONS,
        MAX_TOKENS,
        TIMEOUT,
        MAX_CONSECUTIVE_ERRORS,
        LLM_ERROR,
        UNKNOWN
    }
}