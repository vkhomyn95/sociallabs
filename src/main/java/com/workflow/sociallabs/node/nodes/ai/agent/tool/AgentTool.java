package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import com.workflow.sociallabs.node.nodes.ai.agent.exception.ToolExecutionException;
import lombok.NonNull;

/**
 * I — тип вхідних аргументів (POJO з @JsonProperty)
 * O — тип результату
 *
 * Spring збирає всі AgentTool<??,??> beans автоматично через ToolRegistry.
 */
public interface AgentTool<I extends ToolInput, O extends ToolOutput> {

    /** Унікальне ім'я для LLM function calling */
    String getName();

    String getDescription();

    /** JSON Schema для LLM — будується з типу I через рефлексію */
    ToolSchema getSchema();

    /** Клас вхідного типу — потрібен для десеріалізації */
    Class<I> getInputType();

    /** Основний виклик */
    O execute(@NonNull I input, @NonNull ToolContext context) throws ToolExecutionException;
}
