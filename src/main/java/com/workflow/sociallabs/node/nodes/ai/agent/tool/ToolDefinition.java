package com.workflow.sociallabs.node.nodes.ai.agent.tool;

import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class ToolDefinition {
    String name;
    String description;
    ToolSchema schema;
}
