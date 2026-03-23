package com.workflow.sociallabs.node.nodes.ai.agent.memory.impl;

import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.AgentMemory;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.SessionId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public final class WindowBufferMemory implements AgentMemory {

    // sessionId → history (thread-safe)
    private final ConcurrentHashMap<String, Deque<AgentMessage>> store = new ConcurrentHashMap<>();

    private final int maxWindowSize;

    public WindowBufferMemory(
            @Value("${agent.memory.window.size:20}") int maxWindowSize
    ) {
        this.maxWindowSize = maxWindowSize;
    }

    @Override
    public List<AgentMessage> load(SessionId sessionId) {
        Deque<AgentMessage> history = store.get(sessionId.value());
        if (history == null) return List.of();
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    @Override
    public void save(SessionId sessionId, List<AgentMessage> newMessages) {
        store.compute(sessionId.value(), (key, existing) -> {
            Deque<AgentMessage> deque = existing != null
                    ? existing
                    : new ArrayDeque<>();
            newMessages.forEach(deque::addLast);
            // Обрізаємо до maxWindowSize пар (user + assistant)
            while (deque.size() > maxWindowSize) {
                deque.pollFirst();
            }
            return deque;
        });
        log.debug(
                "Saved {} messages for session {}, total={}",
                newMessages.size(),
                sessionId.value(),
                store.get(sessionId.value()).size()
        );
    }

    @Override
    public void clear(SessionId sessionId) {
        store.remove(sessionId.value());
    }

    @Override
    public MemoryType getType() { return MemoryType.WINDOW_BUFFER; }
}