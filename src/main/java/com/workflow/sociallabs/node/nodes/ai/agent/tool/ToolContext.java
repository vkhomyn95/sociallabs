package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import com.workflow.sociallabs.node.nodes.ai.agent.AgentLimits;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;

@Value
@Builder
public class ToolContext {
    @NonNull Long              workflowId;
    @NonNull String            agentNodeId;
    @NonNull String            executionCorrelationId;  // tracing
    @NonNull Map<String, Object> credentials;      // з workflow
    @NonNull AgentLimits limits;
    @NonNull Clock clock;

    public <T> Optional<T> getCredential(String key, Class<T> type) {
        Object val = credentials.get(key);
        return Optional.ofNullable(val).filter(type::isInstance).map(type::cast);
    }
}
