package com.workflow.sociallabs.node.core;

import lombok.*;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeResult {

    private boolean success;

    /**
     * outputs[0] = main (або true для IF)
     * outputs[1] = false для IF, case1 для SWITCH
     * Звичайна нода: outputs.size() == 1
     * IF нода:       outputs.size() == 2
     * SWITCH нода:   outputs.size() == N
     *
     * Engine не знає семантику — тільки індекси.
     * Connection зберігає sourceOutputIndex.
     */
    private List<List<WorkflowItem>> outputs;

    private String error;
    private String errorStack;
    private Long executionTimeMs;

    // --- фабричні методи ---

    /** Звичайна — один вихід */
    public static NodeResult success(List<WorkflowItem> items) {
        return NodeResult.builder()
                .success(true)
                .outputs(List.of(items))
                .build();
    }

    /** IF / SWITCH — кілька виходів */
    public static NodeResult multiOutput(List<List<WorkflowItem>> outputs) {
        return NodeResult.builder()
                .success(true)
                .outputs(outputs)
                .build();
    }

    public static NodeResult error(String message, String stack) {
        return NodeResult.builder()
                .success(false)
                .error(message)
                .errorStack(stack)
                .build();
    }

    /** Отримати items для порту за індексом */
    public List<WorkflowItem> getOutputItems(int portIndex) {
        if (outputs == null || portIndex >= outputs.size()) {
            return List.of();
        }
        return outputs.get(portIndex);
    }

    /** Shortcut для main (port 0) */
    public List<WorkflowItem> getMainItems() {
        return getOutputItems(0);
    }
}