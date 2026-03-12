package com.workflow.sociallabs.node.nodes.logic.parameters;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.nodes.logic.models.IfCombineLogicOperation;
import com.workflow.sociallabs.node.nodes.logic.models.IfLogicGroup;
import com.workflow.sociallabs.node.parameters.TypedNodeParameters;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName(NodeDiscriminator.Values.IF_LOGIC)
public class IfNodeParameters implements TypedNodeParameters {

    private List<IfLogicGroup> conditions;
    private IfCombineLogicOperation combineOperation; // "AND" | "OR"

    @Override
    public void validate() {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("IF node requires at least one condition");
        }
    }
    @Override
    public NodeDiscriminator getParameterType() { return NodeDiscriminator.IF_LOGIC; }
}
