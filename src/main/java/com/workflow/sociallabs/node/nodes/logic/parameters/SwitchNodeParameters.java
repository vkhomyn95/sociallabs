package com.workflow.sociallabs.node.nodes.logic.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.logic.models.SwitchLogicMode;
import com.workflow.sociallabs.node.nodes.logic.models.SwitchLogicRule;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName(NodeDiscriminator.Values.SWITCH_LOGIC)
public class SwitchNodeParameters implements TypedNodeParameters {

    /**
     * RULES  — кожне правило = окремий вихідний порт
     * EXPRESSION — одне вираження повертає індекс порту
     */
    private SwitchLogicMode mode;

    /**
     * Правила (mode = RULES).
     * rules.get(i) → output port i
     * Останній порт завжди "fallback" (якщо жодне не спрацювало).
     */
    private List<SwitchLogicRule> rules;

    /**
     * Вираз (mode = EXPRESSION).
     * Повинен повернути int — індекс порту.
     * Наприклад: "{{ $json.status }}" → "0" або "1"
     */
    private String expression;

    /**
     * Чи надсилати item у fallback порт якщо жодне правило не спрацювало.
     * За замовчуванням true.
     */
    @Builder.Default
    private boolean fallbackEnabled = true;

    @Override
    public void validate() {
        if (mode == null) {
            throw new IllegalArgumentException("Switch mode is required");
        }
        if (mode == SwitchLogicMode.RULES) {
            if (rules == null || rules.isEmpty()) {
                throw new IllegalArgumentException("Switch rules cannot be empty in RULES mode");
            }
        }
        if (mode == SwitchLogicMode.EXPRESSION) {
            if (expression == null || expression.isBlank()) {
                throw new IllegalArgumentException("Expression is required in EXPRESSION mode");
            }
        }
    }

    @Override
    public NodeDiscriminator getParameterType() {
        return NodeDiscriminator.SWITCH_LOGIC;
    }
}
