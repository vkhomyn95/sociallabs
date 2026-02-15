package com.workflow.sociallabs.node.nodes.telegram.client.listeners;

import com.workflow.sociallabs.node.nodes.telegram.client.models.TelegramClientAuthStateMapper;
import com.workflow.sociallabs.node.nodes.telegram.client.models.TelegramClientSession;
import it.tdlight.client.GenericUpdateHandler;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class TelegramClientAuthRestoreListener implements GenericUpdateHandler<TdApi.Update> {

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

        log.info("[{}] Authorization restore state changed: {}", getSessionId(), simpleName);

        session.setStatus(TelegramClientAuthStateMapper.map(state));

        switch (state) {
            case TdApi.AuthorizationStateWaitPhoneNumber ignored ->
                handleAuthError(new Exception("Session expired, re-authentication phone number required"));
            case TdApi.AuthorizationStateWaitCode ignored ->
                    handleAuthError(new Exception("Session expired, re-authentication code required"));
            case TdApi.AuthorizationStateWaitPassword ignored ->
                    handleAuthError(new Exception("Session expired, re-authentication password required"));
            case TdApi.AuthorizationStateWaitRegistration ignored ->
                    handleAuthError(new Exception("This phone number is not registered. Please register first."));
            case TdApi.AuthorizationStateReady ignored -> {
                log.info("[{}] Restore session completed.", session.getSessionId());

                // Get user info asynchronously
                session.getClient().send(new TdApi.GetMe())
                        .thenAccept(user -> {
                            log.info("[{}] Successfully retrieved user id: {}", getSessionId(), user.id);
                            session.setCurrentUser(user);
                            session.setPhoneNumber(user.phoneNumber);
                            onAuthSuccess.accept(session, user);
                        })
                        .exceptionally(throwable -> {
                            log.error("[{}] Failed to get user info", getSessionId(), throwable);
                            handleAuthError(new Exception("Failed to retrieve user information", throwable));
                            return null;
                        });
            }
            case TdApi.AuthorizationStateClosed ignored ->
                    handleAuthError(new Exception("Session is closed during re-authentication."));
            default ->
                    log.warn("[{}] Unhandled re-authorization state: {}", getSessionId(), simpleName);
        }
    }

    private void handleAuthError(Exception error) {
        onAuthError.accept(session, error);
    }

    private String getSessionId() {
        return session.getSessionId();
    }
}