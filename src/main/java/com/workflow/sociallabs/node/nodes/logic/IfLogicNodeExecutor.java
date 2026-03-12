package com.workflow.sociallabs.node.nodes.logic;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.nodes.logic.models.IfCombineLogicOperation;
import com.workflow.sociallabs.node.nodes.logic.models.IfLogicGroup;
import com.workflow.sociallabs.node.nodes.logic.parameters.IfNodeParameters;
import com.workflow.sociallabs.service.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class IfLogicNodeExecutor extends AbstractNode {

    @Autowired
    private ExpressionEvaluator evaluator;

    public IfLogicNodeExecutor() {
        super(NodeDiscriminator.IF_LOGIC);
    }

    @Override
    protected NodeResult executeInternal(ExecutionContext context) {
        IfNodeParameters params = context.getParameters(IfNodeParameters.class);

        // Валідація
        params.validate();

        List<Map<String, Object>> inputData = context.getInputData();
        List<Map<String, Object>> trueItems = new ArrayList<>();
        List<Map<String, Object>> falseItems = new ArrayList<>();

        for (Map<String, Object> item : inputData) {
            boolean result = evaluateConditions(params, item);
            if (result) {
                trueItems.add(item);
            } else {
                falseItems.add(item);
            }
        }

        Map<String, List<Map<String, Object>>> outputs = new LinkedHashMap<>();
        outputs.put("true", trueItems);
        outputs.put("false", falseItems);

        log.debug("IF node: {} items → true={}, false={}", inputData.size(), trueItems.size(), falseItems.size());

        return NodeResult.multiOutput(outputs);
    }

    private boolean evaluateConditions(IfNodeParameters params, Map<String, Object> item) {
        List<IfLogicGroup> conditions = params.getConditions();
        boolean isAnd = IfCombineLogicOperation.AND.equals(params.getCombineOperation());

        for (IfLogicGroup condition : conditions) {
            Object leftResolved  = evaluator.resolveValue(condition.getLeftValue(), item);
            Object rightResolved = evaluator.resolveValue(condition.getRightValue(), item);

            boolean conditionResult = evaluator.evaluate(
                    leftResolved,
                    condition.getOperation(),
                    rightResolved,
                    condition.getType()
            );

            if (isAnd && !conditionResult) return false;
            if (!isAnd && conditionResult) return true;
        }

        return isAnd; // AND: всі true → true; OR: жодне не спрацювало → false
    }
}
