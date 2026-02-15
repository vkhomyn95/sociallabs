package com.workflow.sociallabs.node.nodes.telegram.client.models;

public enum TelegramClientAuthStatus {

    initializing,
    restoring,

    waiting_tdlib_params,
    waiting_phone,
    waiting_qr,
    waiting_code,
    waiting_password,
    waiting_registration,

    ready,
    closing,
    logging_out,
    closed,

    error
}
