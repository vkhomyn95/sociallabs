package com.workflow.sociallabs.node.nodes.telegram.client.models;

public enum TelegramClientAuthUpdate {
    status,
    phone_required,
    qr_code,
    code_required,
    password_required,
    registration_required,
    success,
    error,
    closing,
    logging_out,
    closed
}
