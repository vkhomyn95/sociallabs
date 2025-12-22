package com.workflow.sociallabs.node.base;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
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
        List<Map<String, Object>> inputData = context.getInputData();
        List<Map<String, Object>> outputData = new ArrayList<>();

        // Обробка кожного елемента
        for (Map<String, Object> item : inputData) {
            try {
                Map<String, Object> result = processItem(item, context);
                if (result != null) {
                    outputData.add(result);
                }
            } catch (Exception e) {
                if (shouldFailOnError(context)) {
                    throw e;
                } else {
                    log.warn("Failed to process item, continuing: {}", e.getMessage());
                    // Можна додати помилку в метадані
                    Map<String, Object> errorItem = new HashMap<>(item);
                    errorItem.put("_error", e.getMessage());
                    outputData.add(errorItem);
                }
            }
        }

        return NodeResult.success(outputData);
    }

    /**
     * Чи повинна node зупинитися при помилці обробки одного елемента
     */
    protected boolean shouldFailOnError(ExecutionContext context) {
        return false;
    }
}