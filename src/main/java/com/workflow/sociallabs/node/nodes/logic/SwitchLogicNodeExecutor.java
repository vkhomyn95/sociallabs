package com.workflow.sociallabs.node.nodes.logic;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import com.workflow.sociallabs.node.core.WorkflowItem;
import com.workflow.sociallabs.node.nodes.logic.models.IfCombineLogicOperation;
import com.workflow.sociallabs.node.nodes.logic.models.IfLogicGroup;
import com.workflow.sociallabs.node.nodes.logic.models.SwitchLogicMode;
import com.workflow.sociallabs.node.nodes.logic.models.SwitchLogicRule;
import com.workflow.sociallabs.node.nodes.logic.parameters.SwitchNodeParameters;
import com.workflow.sociallabs.service.ExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@SuppressWarnings("Duplicates")
public class SwitchLogicNodeExecutor extends AbstractNode {

    public SwitchLogicNodeExecutor() {
        super(NodeDiscriminator.SWITCH_LOGIC);
    }

    @Override
    protected NodeResult executeInternal(ExecutionContext context) {
        SwitchNodeParameters params = context.getParameters(SwitchNodeParameters.class);
        params.validate();

        return params.getMode() == SwitchLogicMode.RULES
                ? executeRulesMode(params, context)
                : executeExpressionMode(params, context);
    }

    // ── RULES mode ────────────────────────────────────────────────────────────

    /**
     * Для кожного item шукаємо перше правило що спрацювало.
     * Item потрапляє в outputs[rule.outputIndex].
     * Якщо жодне не спрацювало — у fallback порт (останній).
     *
     * Кількість портів = maxOutputIndex + 1 (+ 1 fallback якщо enabled).
     */
    private NodeResult executeRulesMode(SwitchNodeParameters params, ExecutionContext context) {
        List<SwitchLogicRule> rules = params.getRules();

        // Визначаємо кількість портів
        int maxIndex = rules.stream()
                .mapToInt(SwitchLogicRule::getOutputIndex)
                .max()
                .orElse(0);

        // fallback завжди останній порт
        int fallbackIndex = maxIndex + 1;
        int totalPorts    = fallbackIndex + (params.isFallbackEnabled() ? 1 : 0);

        // Ініціалізуємо порожні bucket-и для кожного порту
        List<List<WorkflowItem>> outputs = new ArrayList<>(totalPorts);
        for (int i = 0; i < totalPorts; i++) {
            outputs.add(new ArrayList<>());
        }

        for (WorkflowItem item : context.getInputItems()) {
            boolean matched = false;

            for (SwitchLogicRule rule : rules) {
                if (evaluateRule(rule, item.json())) {
                    outputs.get(rule.getOutputIndex()).add(item);
                    matched = true;
                    break; // перше спрацьоване правило — виходимо
                }
            }

            // Жодне не спрацювало → fallback
            if (!matched && params.isFallbackEnabled()) {
                outputs.get(fallbackIndex).add(item);
            }
        }

        logSwitchResult(context.getNodeId(), outputs, "RULES");

        return NodeResult.multiOutput(outputs);
    }

    // ── EXPRESSION mode ───────────────────────────────────────────────────────

    /**
     * Вираз повертає int — індекс порту.
     * Якщо вираз повертає невалідний індекс → fallback.
     */
    private NodeResult executeExpressionMode(SwitchNodeParameters params, ExecutionContext context) {
        String expression = params.getExpression();

        // Спершу проходимо всі items щоб визначити максимальний індекс
        // (щоб знати скільки портів треба створити)
        List<WorkflowItem> items = context.getInputItems();

        // Попередній прохід — обчислюємо індекси
        List<Integer> resolvedIndexes = new ArrayList<>(items.size());
        int maxIndex = 0;

        for (WorkflowItem item : items) {
            int idx = resolveExpressionIndex(expression, item.json());
            resolvedIndexes.add(idx);
            if (idx >= 0) maxIndex = Math.max(maxIndex, idx);
        }

        int fallbackIndex = maxIndex + 1;
        int totalPorts    = fallbackIndex + (params.isFallbackEnabled() ? 1 : 0);

        List<List<WorkflowItem>> outputs = new ArrayList<>(totalPorts);
        for (int i = 0; i < totalPorts; i++) {
            outputs.add(new ArrayList<>());
        }

        // Другий прохід — розкладаємо по портах
        for (int i = 0; i < items.size(); i++) {
            int idx = resolvedIndexes.get(i);

            if (idx >= 0 && idx < fallbackIndex) {
                outputs.get(idx).add(items.get(i));
            } else if (params.isFallbackEnabled()) {
                outputs.get(fallbackIndex).add(items.get(i));
            }
        }

        logSwitchResult(context.getNodeId(), outputs, "EXPRESSION");

        return NodeResult.multiOutput(outputs);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean evaluateRule(SwitchLogicRule rule, Map<String, Object> json) {
        List<IfLogicGroup> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) return false;

        boolean isAnd = IfCombineLogicOperation.AND.equals(rule.getCombineOperation());

        for (IfLogicGroup condition : conditions) {
            Object left  = ExpressionEvaluator.resolveValue(condition.getLeftValue(),  json);
            Object right = ExpressionEvaluator.resolveValue(condition.getRightValue(), json);

            boolean result = ExpressionEvaluator.evaluate(left, condition.getOperation(), right, condition.getType());

            if (isAnd  && !result) return false;
            if (!isAnd &&  result) return true;
        }

        return isAnd;
    }

    /**
     * Резолвить вираз у int-індекс порту.
     * Повертає -1 якщо вираз не вдалося розпарсити.
     */
    private int resolveExpressionIndex(String expression, java.util.Map<String, Object> json) {
        try {
            Object resolved = ExpressionEvaluator.resolveValue(expression, json);
            if (resolved instanceof Number n) return n.intValue();
            if (resolved instanceof String s) return Integer.parseInt(s.trim());
        } catch (Exception e) {
            log.warn("Switch EXPRESSION: failed to resolve index from '{}': {}", expression, e.getMessage());
        }
        return -1; // → fallback
    }

    private void logSwitchResult(String nodeId, List<List<WorkflowItem>> outputs, String mode) {
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Switch node ").append(nodeId).append(" [").append(mode).append("]: ");
            for (int i = 0; i < outputs.size(); i++) {
                sb.append("port[").append(i).append("]=").append(outputs.get(i).size());
                if (i < outputs.size() - 1) sb.append(" ");
            }
            log.debug(sb.toString());
        }
    }
}