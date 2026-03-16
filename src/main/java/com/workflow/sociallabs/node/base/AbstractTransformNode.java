package com.workflow.sociallabs.node.base;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Базовий клас для нод-трансформерів
 * Трансформери перетворюють структуру даних
 */
@Slf4j
public abstract class AbstractTransformNode extends AbstractNode {

    protected AbstractTransformNode(NodeDiscriminator nodeType) {
        super(nodeType);
    }

    /**
     * Трансформувати дані
     */
    protected abstract List<Map<String, Object>> transform(
            List<Map<String, Object>> inputData,
            ExecutionContext context
    ) throws Exception;

    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
//        List<Map<String, Object>> inputData = context.getInputData();
//        List<Map<String, Object>> transformedData = transform(inputData, context);
//        return NodeResult.success(transformedData);
        return null;
    }
}
