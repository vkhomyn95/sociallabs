package com.workflow.sociallabs.node.core;

import com.workflow.sociallabs.domain.model.NodeParameters;
import com.workflow.sociallabs.model.NodeDiscriminator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Представлення ноди в execution graph
 * Містить всю необхідну інформацію для виконання
 */
@Getter
@Builder
@AllArgsConstructor
public class GraphNode {

    private final String nodeId;
    private final NodeDiscriminator discriminator;
    private final NodeParameters parameters;
    private final Map<String, Object> credentials;

    // Попередні ноди (батьки в графі)
    @Builder.Default
    private final List<GraphNode> previousNodes = new ArrayList<>();

    // Наступні ноди (діти в графі)
    @Builder.Default
    private final List<GraphNode> nextNodes = new ArrayList<>();

    /**
     * Чи є нода тригером
     */
    public boolean isTrigger() {
        return previousNodes.isEmpty();
    }

    /**
     * Чи є нода листком (кінцева нода)
     */
    public boolean isLeaf() {
        return nextNodes.isEmpty();
    }

    /**
     * Додати наступну ноду
     */
    public void addNextNode(GraphNode node) {
        if (!nextNodes.contains(node)) {
            nextNodes.add(node);
        }
    }

    /**
     * Додати попередню ноду
     */
    public void addPreviousNode(GraphNode node) {
        if (!previousNodes.contains(node)) {
            previousNodes.add(node);
        }
    }
}
