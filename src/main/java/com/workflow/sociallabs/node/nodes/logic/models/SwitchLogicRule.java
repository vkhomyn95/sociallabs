package com.workflow.sociallabs.node.nodes.logic.models;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwitchLogicRule {

    /**
     * Індекс вихідного порту (0, 1, 2...).
     * outputs[outputIndex] отримає items що пройшли це правило.
     */
    private int outputIndex;

    /** Відображувана назва порту (для UI) */
    private String outputName;

    /**
     * Умови всередині правила — аналогічно до IF ноди.
     * Якщо conditions > 1, використовується combineOperation.
     */
    private List<IfLogicGroup> conditions;

    /**
     * AND / OR між умовами в межах одного правила.
     */
    @Builder.Default
    private IfCombineLogicOperation combineOperation = IfCombineLogicOperation.AND;
}
