package com.workflow.sociallabs.node.nodes.ai.agent.exception;

public class ToolNotFoundException extends RuntimeException {

    private final String toolName;

    public ToolNotFoundException(String toolName) {
        super("No tool registered with name: '" + toolName + "'");
        this.toolName = toolName;
    }

    public String getToolName() { return toolName; }
}