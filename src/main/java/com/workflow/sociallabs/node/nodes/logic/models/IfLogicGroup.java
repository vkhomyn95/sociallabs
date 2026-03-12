package com.workflow.sociallabs.node.nodes.logic.models;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IfLogicGroup {
    private String leftValue;               // "{{$json.status}}" або literal
    private LogicOperation operation;       // EQUALS, NOT_EQUALS, CONTAINS, GT, LT, etc.
    private String rightValue;
    private String type;                    // STRING, NUMBER, BOOLEAN, DATE
}
