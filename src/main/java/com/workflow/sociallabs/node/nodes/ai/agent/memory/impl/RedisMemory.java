package com.workflow.sociallabs.node.nodes.ai.agent.memory.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.workflow.sociallabs.node.nodes.ai.agent.llm.AgentMessage;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.AgentMemory;
import com.workflow.sociallabs.node.nodes.ai.agent.memory.SessionId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public final class RedisMemory implements AgentMemory {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agent.memory.redis.ttl-hours:24}")
    private long ttlHours;

    @Value("${agent.memory.redis.max-messages:50}")
    private int maxMessages;

    private static final String KEY_PREFIX = "agent:memory:";

    @Override
    public List<AgentMessage> load(SessionId sessionId) {
        String key = KEY_PREFIX + sessionId.value();
        List<String> raw = redisTemplate.opsForList()
                .range(key, 0, -1);

        if (raw.isEmpty()) return List.of();

        return raw.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void save(SessionId sessionId, List<AgentMessage> newMessages) {
        String key = KEY_PREFIX + sessionId.value();

        List<String> serialized = newMessages.stream()
                .map(this::serialize)
                .filter(Objects::nonNull)
                .toList();

        if (serialized.isEmpty()) return;

        // RPUSH — додаємо в кінець списку
        redisTemplate.opsForList().rightPushAll(key, serialized);

        // Обрізаємо до maxMessages (зліва — найстаріші)
        long size = redisTemplate.opsForList().size(key);
        if (size > maxMessages) {
            redisTemplate.opsForList().trim(key, size - maxMessages, -1);
        }

        // Оновлюємо TTL
        redisTemplate.expire(key, Duration.ofHours(ttlHours));

        log.debug("Saved {} messages to Redis for session {}", serialized.size(), sessionId.value());
    }

    @Override
    public void clear(SessionId sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId.value());
    }

    @Override
    public MemoryType getType() { return MemoryType.REDIS; }

    private String serialize(AgentMessage msg) {
        try {
            Map<String, Object> wrapper = Map.of(
                    "role",    msg.role().name(),
                    "content", objectMapper.writeValueAsString(msg)
            );
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize", e); return null;
        }
    }

    private AgentMessage deserialize(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            AgentMessage.Role role = AgentMessage.Role.valueOf(
                    node.get("role").asText());
            String content = node.get("content").asText();
            return switch (role) {
                case USER      -> objectMapper.readValue(content, AgentMessage.UserMessage.class);
                case ASSISTANT -> objectMapper.readValue(content, AgentMessage.AssistantMessage.class);
                case TOOL      -> objectMapper.readValue(content, AgentMessage.ToolResultMessage.class);
            };
        } catch (Exception e) {
            log.warn("Failed to deserialize memory entry", e); return null;
        }
    }
}
