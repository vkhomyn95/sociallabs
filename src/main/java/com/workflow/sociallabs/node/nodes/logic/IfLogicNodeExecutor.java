package com.workflow.sociallabs.node.nodes.logic;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.core.WorkflowItem;
import com.workflow.sociallabs.node.nodes.logic.models.IfCombineLogicOperation;
import com.workflow.sociallabs.node.nodes.logic.models.IfLogicGroup;
import com.workflow.sociallabs.node.nodes.logic.parameters.IfNodeParameters;
import com.workflow.sociallabs.service.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@SuppressWarnings("Duplicates")
public class IfLogicNodeExecutor extends AbstractNode {

    public IfLogicNodeExecutor() {
        super(NodeDiscriminator.IF_LOGIC);
    }

    @Override
    protected NodeResult executeInternal(ExecutionContext context) {
        IfNodeParameters params = context.getParameters(IfNodeParameters.class);
        params.validate();

        List<WorkflowItem> trueItems  = new ArrayList<>();
        List<WorkflowItem> falseItems = new ArrayList<>();

        for (WorkflowItem item : context.getInputItems()) {
            if (evaluateConditions(params, item.json())) {
                trueItems.add(item);
            } else {
                falseItems.add(item);
            }
        }

        log.debug("IF node {}: total={} true={} false={}",
                context.getNodeId(),
                context.getInputItems().size(),
                trueItems.size(),
                falseItems.size());

        // outputs[0] = true branch, outputs[1] = false branch
        return NodeResult.multiOutput(List.of(trueItems, falseItems));
    }

    private boolean evaluateConditions(IfNodeParameters params, java.util.Map<String, Object> json) {
        List<IfLogicGroup> conditions = params.getConditions();
        boolean isAnd = IfCombineLogicOperation.AND.equals(params.getCombineOperation());

        for (IfLogicGroup condition : conditions) {
            Object left  = ExpressionEvaluator.resolveValue(condition.getLeftValue(),  json);
            Object right = ExpressionEvaluator.resolveValue(condition.getRightValue(), json);

            boolean result = ExpressionEvaluator.evaluate(left, condition.getOperation(), right, condition.getType());

            if (isAnd  && !result) return false; // AND: один false → одразу false
            if (!isAnd &&  result) return true;  // OR:  один true  → одразу true
        }

        return isAnd; // AND: всі пройшли → true | OR: жоден не спрацював → false
    }
}
