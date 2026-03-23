package com.workflow.sociallabs.node.nodes.ai.agent.exception;

import com.workflow.sociallabs.node.nodes.ai.agent.memory.AgentMemory;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.SessionId;

public class AgentMemoryException extends RuntimeException {

    private final AgentMemory.MemoryType memoryType;
    private final SessionId sessionId;

    public AgentMemoryException(
            String message,
            AgentMemory.MemoryType memoryType,
            SessionId sessionId,
            Throwable cause
    ) {
        super(message, cause);
        this.memoryType = memoryType;
        this.sessionId = sessionId;
    }

    public AgentMemory.MemoryType getMemoryType() {
        return memoryType;
    }

    public SessionId getSessionId() {
        return sessionId;
    }
}
