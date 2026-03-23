package com.workflow.sociallabs.node.base;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.core.WorkflowItem;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Базовий клас для нод-дій
 * Дії виконують певні операції з даними
 */
@Slf4j
public abstract class AbstractActionNode extends AbstractNode {

    protected AbstractActionNode(NodeDiscriminator nodeType) {
        super(nodeType);
    }

    /**
     * Виконати дію для одного елемента даних
     */
    protected abstract Map<String, Object> processItem(
            Map<String, Object> item,
            ExecutionContext context
    ) throws Exception;

    /**
     * Стандартна реалізація - обробляє всі вхідні елементи
     */
    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
        List<WorkflowItem> inputItems = context.getInputItems();
        List<WorkflowItem> outputItems = new ArrayList<>(inputItems.size());

        for (WorkflowItem item : inputItems) {
            try {
                Map<String, Object> resultJson = processItem(item.json(), context);
                if (resultJson != null) {
                    // Зберігаємо binary з вхідного item якщо є
                    outputItems.add(new WorkflowItem(resultJson, item.binary()));
                }
            } catch (Exception e) {
                if (shouldFailOnError(context)) {
                    throw e;
                }
                log.warn("Node {}: failed to process item, continuing: {}", context.getNodeId(), e.getMessage());

                // Додаємо item з позначкою помилки, не губимо дані
                Map<String, Object> errorJson = new HashMap<>(item.json());
                errorJson.put("_error", e.getMessage());
                outputItems.add(new WorkflowItem(errorJson, item.binary()));
            }
        }

        return NodeResult.success(outputItems);
    }

    /**
     * Чи повинна node зупинитися при помилці обробки одного елемента
     */
    protected boolean shouldFailOnError(ExecutionContext context) {
        return false;
    }
}