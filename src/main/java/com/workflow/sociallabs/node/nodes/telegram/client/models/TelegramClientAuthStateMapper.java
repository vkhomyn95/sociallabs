package com.workflow.sociallabs.node.nodes.telegram.client.models;

import it.tdlight.jni.TdApi;

import java.util.Map;

public final class TelegramClientAuthStateMapper {

    private TelegramClientAuthStateMapper() {}

    public static final Map<Class<? extends TdApi.AuthorizationState>, TelegramClientAuthStatus> STATE_TO_STATUS =
            Map.ofEntries(
                    Map.entry(TdApi.AuthorizationStateWaitTdlibParameters.class, TelegramClientAuthStatus.waiting_tdlib_params),
                    Map.entry(TdApi.AuthorizationStateWaitPhoneNumber.class, TelegramClientAuthStatus.waiting_phone),
                    Map.entry(TdApi.AuthorizationStateWaitOtherDeviceConfirmation.class, TelegramClientAuthStatus.waiting_qr),
                    Map.entry(TdApi.AuthorizationStateWaitCode.class, TelegramClientAuthStatus.waiting_code),
                    Map.entry(TdApi.AuthorizationStateWaitPassword.class, TelegramClientAuthStatus.waiting_password),
                    Map.entry(TdApi.AuthorizationStateWaitRegistration.class, TelegramClientAuthStatus.waiting_registration),
                    Map.entry(TdApi.AuthorizationStateReady.class, TelegramClientAuthStatus.ready),
                    Map.entry(TdApi.AuthorizationStateClosing.class, TelegramClientAuthStatus.closing),
                    Map.entry(TdApi.AuthorizationStateLoggingOut.class, TelegramClientAuthStatus.logging_out),
                    Map.entry(TdApi.AuthorizationStateClosed.class, TelegramClientAuthStatus.closed)
            );

    public static TelegramClientAuthStatus map(TdApi.AuthorizationState state) {
        return STATE_TO_STATUS.getOrDefault(state.getClass(), TelegramClientAuthStatus.error);
    }
}
