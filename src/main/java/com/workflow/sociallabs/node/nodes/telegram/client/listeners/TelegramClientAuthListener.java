package com.workflow.sociallabs.node.nodes.telegram.client.listeners;

import com.workflow.sociallabs.node.nodes.telegram.client.models.TelegramClientAuthStateMapper;
import com.workflow.sociallabs.node.nodes.telegram.client.models.TelegramClientAuthUpdate;
import com.workflow.sociallabs.node.nodes.telegram.client.models.TelegramClientSession;
import it.tdlight.client.GenericUpdateHandler;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class TelegramClientAuthListener implements GenericUpdateHandler<TdApi.Update> {

    private final TelegramClientSession session;
    private final BiConsumer<TelegramClientSession, TdApi.User> onAuthSuccess;
    private final BiConsumer<TelegramClientSession, Exception> onAuthError;

    @Override
    public void onUpdate(TdApi.Update update) {
        if (update instanceof TdApi.UpdateAuthorizationState state) {
            onUpdateAuthorizationState(state.authorizationState);
        }
    }

    private void onUpdateAuthorizationState(TdApi.AuthorizationState state) {
        String simpleName = state.getClass().getSimpleName();

        log.info("[{}] Authorization state changed: {}", getSessionId(), simpleName);

        session.setStatus(TelegramClientAuthStateMapper.map(state));

        switch (state) {
            case TdApi.AuthorizationStateWaitTdlibParameters ignored ->
                    sendUpdate(
                            TelegramClientAuthUpdate.status,
                            Map.of("message", "Initializing session...")
                    );
            case TdApi.AuthorizationStateWaitPhoneNumber ignored -> {
                sendUpdate(
                        TelegramClientAuthUpdate.phone_required,
                        Map.of("message", "Phone number required")
                );
                executePhoneNumber();
            }
            case TdApi.AuthorizationStateWaitOtherDeviceConfirmation qr ->
                    sendUpdate(
                            TelegramClientAuthUpdate.qr_code,
                            Map.of(
                                    "link", qr.link,
                                    "message", "Scan QR code in Telegram app"
                            )
                    );
            case TdApi.AuthorizationStateWaitCode code ->
                    sendUpdate(
                            TelegramClientAuthUpdate.code_required,
                            Map.of(
                                    "timeout", code.codeInfo.timeout,
                                    "type", code.codeInfo.type.getClass().getSimpleName(),
                                    "message", "Enter confirmation code"
                            )
                    );
            case TdApi.AuthorizationStateWaitPassword password ->
                    sendUpdate(
                            TelegramClientAuthUpdate.password_required,
                            Map.of(
                                    "hint", password.passwordHint,
                                    "hasRecoveryEmail", password.hasRecoveryEmailAddress,
                                    "message", "Enter 2FA password"
                            )
                    );
            case TdApi.AuthorizationStateWaitRegistration ignored ->
                    sendUpdate(
                            TelegramClientAuthUpdate.registration_required,
                            Map.of("message", "This phone number is not registered. Please register first.")
                    );
            case TdApi.AuthorizationStateReady ignored ->
                    executeAuthorizationReady();
            case TdApi.AuthorizationStateClosed ignored ->
                    sendUpdate(
                            TelegramClientAuthUpdate.closed,
                            Map.of("message", "Session closed")
                    );
            case TdApi.AuthorizationStateClosing ignored ->
                    sendUpdate(
                            TelegramClientAuthUpdate.closing,
                            Map.of("message", "Closing session...")
                    );
            case TdApi.AuthorizationStateLoggingOut ignored ->
                    sendUpdate(
                            TelegramClientAuthUpdate.status,
                            Map.of("message", "Logging out...")
                    );
            default ->
                    log.warn("[{}] Unhandled authorization state: {}", getSessionId(), simpleName);
        }
    }

    private void sendUpdate(TelegramClientAuthUpdate type, Map<String, Object> payload) {
        session.getUpdatesSink().tryEmitNext(
                Map.of(
                        "type", type.name(),
                        "status", session.getStatus().name(),
                        "data", payload
                )
        );
    }

    private void executePhoneNumber() {
        if (session.getPhoneNumber() == null || session.getPhoneNumber().isEmpty()) return;

        session.getClient().send(new TdApi.SetAuthenticationPhoneNumber(session.getPhoneNumber(), null));
    }

    private void executeAuthorizationReady() {
        log.info("[{}] Authentication completed.", session.getSessionId());

        sendUpdate(
                TelegramClientAuthUpdate.status,
                Map.of("message", "Retrieving user information...")
        );

        // Get user info asynchronously
        session.getClient().send(new TdApi.GetMe())
                .thenAccept(user -> {
                    log.info("[{}] Successfully retrieved user id: {}", getSessionId(), user.id);
                    session.setCurrentUser(user);
                    session.setPhoneNumber(user.phoneNumber);
                    onAuthSuccess.accept(session, user);
                })
                .exceptionally(throwable -> {
                    log.error("[{}] Failed to get user info }", getSessionId(), throwable);
                    handleAuthError(new Exception("Failed to retrieve user information", throwable));
                    return null;
                });
    }

    private void handleAuthError(Exception error) {
        sendUpdate(
                TelegramClientAuthUpdate.error,
                Map.of(
                        "message", "Authentication error: " + error.getMessage(),
                        "error", error.getClass().getSimpleName()
                )
        );
        onAuthError.accept(session, error);
    }

    private String getSessionId() {
        return session.getSessionId();
    }
}