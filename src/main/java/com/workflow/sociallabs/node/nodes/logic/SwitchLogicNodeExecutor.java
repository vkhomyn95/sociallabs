package com.workflow.sociallabs.node.nodes.logic;

import com.workflow.sociallabs.model.NodeDiscriminator;
import com.workflow.sociallabs.node.base.AbstractNode;
import com.workflow.sociallabs.node.core.ExecutionContext;
import com.workflow.sociallabs.node.core.NodeResult;
import org.springframework.stereotype.Component;

@Component
public class SwitchLogicNodeExecutor extends AbstractNode {

    public SwitchLogicNodeExecutor() {
        super(NodeDiscriminator.SWITCH_LOGIC);
    }

    @Override
    protected NodeResult executeInternal(ExecutionContext context) throws Exception {
        return null;
    }
}
