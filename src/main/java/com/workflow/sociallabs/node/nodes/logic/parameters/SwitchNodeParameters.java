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

    private SwitchLogicMode mode;             // "rules" | "expression"
    private String switchValue;      // "{{$json.status}}" — що перевіряємо
    private List<SwitchLogicRule> rules;
    private boolean fallbackToDefault; // чи є default output

    @Override
    public void validate() { /* ... */ }

    @Override
    public NodeDiscriminator getParameterType() { return NodeDiscriminator.SWITCH_LOGIC; }
}
