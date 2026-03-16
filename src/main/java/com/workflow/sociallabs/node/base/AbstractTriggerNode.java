package com.workflow.sociallabs.node.base;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.core.WorkflowItem;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
     * Тригер при execute просто збагачує перший вхідний item
     * і передає його далі як один output item.
     */
    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
        Map<String, Object> json = context.getInputItems().isEmpty()
                ? new HashMap<>()
                : new HashMap<>(context.getInputItems().get(0).json());

        // Додаємо службові поля тригера
        json.put("_triggerTime", Instant.now().toString());
        json.put("_triggerNode", context.getNodeId());

        return NodeResult.success(WorkflowItem.single(json));
    }
}