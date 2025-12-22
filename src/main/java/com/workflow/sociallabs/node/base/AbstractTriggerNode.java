package com.workflow.sociallabs.node.base;

import com.workflow.sociallabs.model.NodeDiscriminator;
import lombok.extern.slf4j.Slf4j;

import com.workflow.sociallabs.node.core.*;
import com.workflow.sociallabs.domain.enums.NodeType;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Базовий клас для нод-тригерів
 * Тригери запускають workflow при певній події
 */
@Slf4j
public abstract class AbstractTriggerNode extends AbstractNode {

    protected AbstractTriggerNode(NodeDiscriminator nodeType) {
        super(nodeType);
    }

    /**
     * Активувати тригер
     * @return true якщо тригер успішно активовано
     */
    public abstract boolean activate(ExecutionContext context) throws Exception;

    /**
     * Деактивувати тригер
     */
    public abstract void deactivate(ExecutionContext context) throws Exception;

    /**
     * Для тригерів execute викликається при отриманні події
     */
    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
        // Тригери зазвичай просто передають дані далі
        List<Map<String, Object>> triggerData = context.getInputData();

        if (triggerData == null || triggerData.isEmpty()) {
            triggerData = Collections.singletonList(new HashMap<>());
        }

        // Можна додати додаткові дані специфічні для тригера
        Map<String, Object> enrichedData = enrichTriggerData(triggerData.get(0), context);

        return NodeResult.success(enrichedData);
    }

    /**
     * Збагатити дані від тригера додатковою інформацією
     */
    protected Map<String, Object> enrichTriggerData(Map<String, Object> data, ExecutionContext context) {
        Map<String, Object> enriched = new HashMap<>(data);
        enriched.put("triggerTime", Instant.now().toString());
        enriched.put("triggerNode", context.getNodeId());
        return enriched;
    }
}