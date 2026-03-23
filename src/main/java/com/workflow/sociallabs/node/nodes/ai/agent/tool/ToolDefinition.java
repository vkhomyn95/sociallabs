package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import lombok.Value;

@Value
public class ToolDefinition {
    String name;
    String description;
    ToolSchema schema;
}