package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import com.workflow.sociallabs.node.nodes.ai.agent.exception.ToolNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public final class ToolRegistry {

    // Raw type тут допустимий — ми контролюємо через getName()
    private final Map<String, AgentTool<?, ?>> registry;

    /** Spring inject всіх AgentTool бінів автоматично */
    public ToolRegistry(List<AgentTool<?, ?>> tools) {
        this.registry = tools.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AgentTool::getName,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate tool: " + a.getName());
                        }
                ));

        log.info("ToolRegistry initialized with {} tools: {}", registry.size(), registry.keySet());
    }

    @SuppressWarnings("unchecked")
    public <I extends ToolInput, O extends ToolOutput> AgentTool<I, O> get(String name) {
        AgentTool<?, ?> tool = registry.get(name);
        if (tool == null) throw new ToolNotFoundException("Unknown tool: " + name);
        return (AgentTool<I, O>) tool;
    }

    public List<ToolDefinition> getDefinitions(Collection<String> toolNames) {
        return toolNames.stream()
                .map(this::get)
                .map(t -> new ToolDefinition(t.getName(), t.getDescription(), t.getSchema()))
                .toList();
    }

    public Set<String> getAllNames() {
        return registry.keySet();
    }

    public boolean exists(String name) { return registry.containsKey(name); }
}
