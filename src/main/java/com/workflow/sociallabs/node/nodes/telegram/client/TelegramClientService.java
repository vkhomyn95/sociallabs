package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.domain.entity.Credential;
import com.workflow.sociallabs.domain.enums.CredentialType;
import com.workflow.sociallabs.domain.repository.CredentialRepository;
import com.workflow.sociallabs.node.nodes.telegram.client.listeners.TelegramClientAuthListener;
import com.workflow.sociallabs.node.nodes.telegram.client.listeners.TelegramClientAuthRestoreListener;
import com.workflow.sociallabs.node.nodes.telegram.client.models.*;
import com.workflow.sociallabs.security.CredentialEncryption;
import com.workflow.sociallabs.utility.MapSerializer;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class TelegramClientService {

    private final CredentialRepository credentialRepository;
    private final CredentialEncryption encryption;

    @Value("${spring.telegram.client.database-directory}")
    private String databaseDirectory;
    @Value("${spring.telegram.client.download-directory}")
    private String downloadDirectory;

    // Активні сесії аутентифікації
    private final Map<String, TelegramClientSession> activeSessions = new ConcurrentHashMap<>();

    public SimpleTelegramClient getSession(String sessionId) {
        TelegramClientSession session = activeSessions.get(sessionId);

        if (session == null) return null;

        return session.getClient();
    }

    /**
     * Почати нову сесію аутентифікації
     */
    public String startAuthSession(
            String apiId,
            String apiHash,
            String phoneNumber,
            TelegramClientAuthMethod authMethod
    ) {
        String sessionId = UUID.randomUUID().toString();

        TelegramClientSession session = TelegramClientSession.builder()
                .sessionId(sessionId)
                .apiId(Integer.parseInt(apiId))
                .apiHash(apiHash)
                .phoneNumber(phoneNumber)
                .authMethod(authMethod)
                .status(TelegramClientAuthStatus.initializing)
                .updatesSink(Sinks.many().multicast().onBackpressureBuffer())
                .build();

        activeSessions.put(sessionId, session);

        // Запустити аутентифікацію в окремому потоці
        CompletableFuture.runAsync(() -> initializeSession(session));

        return sessionId;
    }

    /**
     * Ініціалізація Telegram клієнта
     */
    private void initializeSession(TelegramClientSession session) {
        log.info("[{}] Initializing Telegram client", session.getSessionId());

        try {
            TDLibSettings settings = TDLibSettings.create(new APIToken(session.getApiId(), session.getApiHash()));

            settings.setDatabaseDirectoryPath(Paths.get(databaseDirectory, session.getSessionId()));
            settings.setDownloadedFilesDirectoryPath(Paths.get(downloadDirectory, session.getSessionId()));

            SimpleTelegramClientFactory factory = new SimpleTelegramClientFactory();
            session.setFactory(factory);

            SimpleTelegramClientBuilder builder = factory.builder(settings);

            SimpleTelegramClient client = builder.build(createAuthSupplier(session));
            session.setClient(client);

            client.addUpdatesHandler(new TelegramClientAuthListener(session, this::onAuthSuccess, this::onAuthError));
        } catch (Exception e) {
            log.error("[{}] Failed initialize Telegram client", session.getSessionId(), e);
            onAuthError(session, e);
        }
    }

    /**
     * Handle successful authentication
     */
    private void onAuthSuccess(TelegramClientSession session, TdApi.User user) {
        log.info("[{}] Saving credentials for user: {}", session.getSessionId(), session.getPhoneNumber());

        Map<String, Object> credentials = Map.of(
                TelegramClientCredentialKeys.SESSION_ID, session.getSessionId(),
                TelegramClientCredentialKeys.AUTH_METHOD, session.getAuthMethod(),
                TelegramClientCredentialKeys.API_ID, String.valueOf(session.getApiId()),
                TelegramClientCredentialKeys.API_HASH, session.getApiHash(),
                TelegramClientCredentialKeys.PHONE_NUMBER, session.getPhoneNumber(),
                TelegramClientCredentialKeys.USER_ID, user.id,
                TelegramClientCredentialKeys.USERNAME, user.usernames.editableUsername
        );

        try {
            String encryptedData = encryption.encrypt(MapSerializer.serializeToJson(credentials));

            Credential credential = credentialRepository.save(
                    Credential.builder()
                            .name("Telegram - " + session.getPhoneNumber())
                            .type(CredentialType.TELEGRAM_CLIENT)
                            .encryptedData(encryptedData)
                            .description("Authorized via " + session.getAuthMethod())
                            .build()
            );

            // Send success update to frontend
            sendUpdate(session, TelegramClientAuthUpdate.success, Map.of(
                    "credentialId", credential.getId(),
                    "message", "Authentication successful",
                    "user", Map.of(
                            "id", user.id,
                            "firstName", user.firstName,
                            "lastName", user.lastName,
                            "username", "Here username"
                    )
            ));

            log.info("Credentials saved successfully for session: {}", session.getSessionId());
        } catch (Exception e) {
            log.error("Error saving credentials for session: {}", session.getSessionId(), e);
            onAuthError(session, new Exception("Failed to save credentials: " + e.getMessage(), e));
        }
    }

    /**
     * Handle authentication error
     */
    private void onAuthError(TelegramClientSession session, Exception error) {
        log.error("Authentication error for session {}: {}", session.getSessionId(), error.getMessage());

        sendUpdate(session, TelegramClientAuthUpdate.error, Map.of(
                "message", error.getMessage(),
                "error", error.getClass().getSimpleName()
        ));

        // Optionally clean up the session
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // Give time for error message to be received
                cancelAuthSession(session.getSessionId());
            } catch (Exception e) {
                log.error("Error cleaning up failed session", e);
            }
        });
    }

    /**
     * Створити authentication supplier
     */
    private SimpleAuthenticationSupplier<?> createAuthSupplier(TelegramClientSession session) {
        if (TelegramClientAuthMethod.qrcode.equals(session.getAuthMethod())) {
            return AuthenticationSupplier.qrCode();
        } else {
            return AuthenticationSupplier.user(session.getPhoneNumber());
        }
    }

    /**
     * Stream updates for frontend
     */
    public Flux<Map<String, Object>> getAuthUpdatesStream(String sessionId) {
        TelegramClientSession session = activeSessions.get(sessionId);
        if (session == null) {
            return Flux.just(Map.of(
                    "type", "error",
                    "message", "Session not found"
            ));
        }

        return session.getUpdatesSink().asFlux();
    }

    /**
     * Відправити код
     */
    public void submitAuthCode(String sessionId, String code) {
        TelegramClientSession session = activeSessions.get(sessionId);

        Optional.ofNullable(session).ifPresent(s -> s.getClient().send(new TdApi.CheckAuthenticationCode(code)));
    }

    /**
     * Відправити пароль
     */
    public void submitAuthPassword(String sessionId, String password) {
        TelegramClientSession session = activeSessions.get(sessionId);

        Optional.ofNullable(session).ifPresent(s -> s.getClient().send(new TdApi.CheckAuthenticationPassword(password)));
    }

    /**
     * Скасувати сесію
     */
    public void cancelAuthSession(String sessionId) {
        TelegramClientSession session = activeSessions.remove(sessionId);
        if (session != null) {
            if (session.getClient() != null) {
                try {
                    session.getClient().send(new TdApi.Close()).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("[{}] Error closing telegram client", session.getSessionId(), e);
                }
            }
            if (session.getFactory() != null) {
                try {
                    session.getFactory().close();
                } catch (Exception e) {
                    log.error("[{}] Error closing telegram client factory", session.getSessionId(), e);
                }
            }
        }
    }

    /**
     * Статус сесії
     */
    public Map<String, Object> getSessionStatus(String sessionId) {
        TelegramClientSession session = activeSessions.get(sessionId);
        if (session == null) {
            return Map.of("found", false);
        }

        return Map.of(
                "found", true,
                "status", session.getStatus(),
                "sessionId", sessionId
        );
    }

    /**
     * Відправити update на frontend
     */
    private void sendUpdate(TelegramClientSession session, TelegramClientAuthUpdate type, Map<String, Object> data) {
        Map<String, Object> update = new HashMap<>(data);
        update.put("type", type.name());
        update.put("timestamp", System.currentTimeMillis());

        session.getUpdatesSink().tryEmitNext(update);
    }

    /**
     * Відновити сесію аутентифікації
     */
//    @PostConstruct
    public void restoreAuthSession() {
        log.info("Initializing existing Telegram client sessions from database...");

        List<Credential> credentials = credentialRepository.findByType(CredentialType.TELEGRAM_CLIENT);

        for (Credential credential : credentials) {
            try {
                Map<String, Object> data = MapSerializer.deserializeFromJson(
                        encryption.decrypt(credential.getEncryptedData())
                );

                if (data == null) continue;

                String sessionId = (String) data.get(TelegramClientCredentialKeys.SESSION_ID);
                String phoneNumber = (String) data.get(TelegramClientCredentialKeys.PHONE_NUMBER);
                String authMethod = (String) data.get(TelegramClientCredentialKeys.AUTH_METHOD);

                String apiHash = (String) data.get(TelegramClientCredentialKeys.API_HASH);
                String apiId = (String) data.get(TelegramClientCredentialKeys.API_ID);

                log.info("Restoring telegram client session: {}, credential: {}", sessionId, credential.getId());

                TelegramClientSession session = TelegramClientSession.builder()
                        .sessionId(sessionId)
                        .apiId(Integer.parseInt(apiId))
                        .apiHash(apiHash)
                        .phoneNumber(phoneNumber)
                        .authMethod(TelegramClientAuthMethod.valueOf(authMethod))
                        .status(TelegramClientAuthStatus.restoring)
                        .updatesSink(Sinks.many().multicast().onBackpressureBuffer())
                        .build();

                activeSessions.put(sessionId, session);

                TDLibSettings settings = TDLibSettings.create(new APIToken(session.getApiId(), session.getApiHash()));

                settings.setDatabaseDirectoryPath(Paths.get(databaseDirectory, session.getSessionId()));
                settings.setDownloadedFilesDirectoryPath(Paths.get(downloadDirectory, session.getSessionId()));

                SimpleTelegramClientFactory factory = new SimpleTelegramClientFactory();
                session.setFactory(factory);

                SimpleTelegramClientBuilder builder = factory.builder(settings);

                // Для відновлення можна використати будь-який authentication supplier
                // TDLib автоматично використає збережену сесію
                SimpleTelegramClient client = builder.build(AuthenticationSupplier.user(""));
                session.setClient(client);

                client.addUpdatesHandler(new TelegramClientAuthRestoreListener(session, this::onAuthRestoreSuccess, this::onAuthRestoreError));

            } catch (Exception e) {
                log.error("Failed to restore telegram client session for credential {}: {}", credential.getId(), e.getMessage());
            }
        }
    }

    /**
     * Handle successful authentication
     */
    private void onAuthRestoreSuccess(TelegramClientSession session, TdApi.User user) {
        log.info("Authentication restore success for session {}", session.getSessionId());

        // TODO: 28.01.26 Maybe run workflow trigger listeners
    }

    /**
     * Handle authentication error
     */
    private void onAuthRestoreError(TelegramClientSession session, Exception error) {
        log.error("Authentication restore error for session {}: {}", session.getSessionId(), error.getMessage());

        // Optionally clean up the session
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // Give time for error message to be received
                cancelAuthSession(session.getSessionId());
            } catch (Exception e) {
                log.error("[{}] Error cleaning up failed session", session.getSessionId(), e);
            }
        });
    }
}