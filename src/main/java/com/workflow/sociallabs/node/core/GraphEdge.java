package com.workflow.sociallabs.node.core;

public record GraphEdge(
        String sourceNodeId,
        int sourceOutputIndex,   // який порт виходу
        String targetNodeId,
        int targetInputIndex     // який порт входу
) {}
