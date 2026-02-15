package com.workflow.sociallabs.domain.enums;

public enum NodeTriggerType {
    POLLING,      // Періодичний запит
    WEBHOOK,      // Webhook
    EVENT,        // Event-driven
    SCHEDULE      // За розкладом
}
