package com.workflow.sociallabs.service;

import com.workflow.sociallabs.node.nodes.logic.models.LogicOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ExpressionEvaluator {

    /**
     * Резолвить {{$json.field}} вирази з даних
     */
    public static Object resolveValue(String expression, Map<String, Object> data) {
        if (expression == null) return null;

        // Literal — повертаємо як є
        if (!expression.contains("{{")) return expression;

        // Простий resolver для {{$json.fieldName}}
        String resolved = expression;
        Pattern pattern = Pattern.compile("\\{\\{\\$json\\.([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(expression);

        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = getNestedValue(data, path);
            resolved = resolved.replace(matcher.group(0), value != null ? value.toString() : "");
        }

        return resolved;
    }

    private static Object getNestedValue(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else return null;
        }
        return current;
    }

    /**
     * Виконати порівняння двох значень
     */
    public static boolean evaluate(Object left, LogicOperation operation, Object right, String type) {
        if (left == null) return false;

        return switch (operation) {
            case EQUALS          -> String.valueOf(left).equals(String.valueOf(right));
            case NOT_EQUALS      -> !String.valueOf(left).equals(String.valueOf(right));
            case CONTAINS        -> String.valueOf(left).contains(String.valueOf(right));
            case NOT_CONTAINS    -> !String.valueOf(left).contains(String.valueOf(right));
            case STARTS_WITH     -> String.valueOf(left).startsWith(String.valueOf(right));
            case ENDS_WITH       -> String.valueOf(left).endsWith(String.valueOf(right));
            case IS_EMPTY        -> String.valueOf(left).isBlank();
            case IS_NOT_EMPTY    -> !String.valueOf(left).isBlank();
            case GT              -> compareNumbers(left, right) > 0;
            case LT              -> compareNumbers(left, right) < 0;
            case GTE             -> compareNumbers(left, right) >= 0;
            case LTE             -> compareNumbers(left, right) <= 0;
            case REGEX           -> String.valueOf(left).matches(String.valueOf(right));
            case IS_TRUE         -> Boolean.parseBoolean(String.valueOf(left));
            case IS_FALSE        -> !Boolean.parseBoolean(String.valueOf(left));
            default -> false;
        };
    }

    private static int compareNumbers(Object left, Object right) {
        try {
            return Double.compare(
                    Double.parseDouble(String.valueOf(left)),
                    Double.parseDouble(String.valueOf(right))
            );
        } catch (NumberFormatException e) {
            return String.valueOf(left).compareTo(String.valueOf(right));
        }
    }
}
