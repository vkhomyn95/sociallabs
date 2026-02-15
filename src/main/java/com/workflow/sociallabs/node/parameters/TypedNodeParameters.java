package com.workflow.sociallabs.node.parameters;

import com.workflow.sociallabs.model.NodeDiscriminator;


public interface TypedNodeParameters {

    /**
     * Валідація параметрів
     */
    void validate() throws IllegalArgumentException;

    /**
     * Отримати тип параметрів (для дискримінатора)
     */
    NodeDiscriminator getParameterType();
}