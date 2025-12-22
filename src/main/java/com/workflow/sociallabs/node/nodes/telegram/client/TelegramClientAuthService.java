package com.workflow.sociallabs.node.nodes.telegram.client;

import com.workflow.sociallabs.domain.entity.Credential;
import com.workflow.sociallabs.domain.enums.CredentialType;
import com.workflow.sociallabs.domain.repository.CredentialRepository;
import com.workflow.sociallabs.security.CredentialEncryption;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientAuthService {

    private final CredentialRepository credentialRepository;
    private final CredentialEncryption encryption;

    // Активні сесії аутентифікації
    private final Map<String, AuthSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Почати нову сесію аутентифікації
     */
    public String startAuthSession(String apiId, String apiHash, String phoneNumber, String authMethod) {
        String sessionId = UUID.randomUUID().toString();

        AuthSession session = new AuthSession();
        session.sessionId = sessionId;
        session.apiId = Integer.parseInt(apiId);
        session.apiHash = apiHash;
        session.phoneNumber = phoneNumber;
        session.authMethod = authMethod;
        session.status = "initializing";
        session.updatesSink = Sinks.many().multicast().onBackpressureBuffer();

        activeSessions.put(sessionId, session);

        // Запустити аутентифікацію в окремому потоці
        CompletableFuture.runAsync(() -> initializeTelegramClient(session));

        return sessionId;
    }

    /**
     * Stream updates для фронтенду
     */
    public Flux<Map<String, Object>> getAuthUpdatesStream(String sessionId) {
        AuthSession session = activeSessions.get(sessionId);
        if (session == null) {
            return Flux.just(Map.of(
                    "type", "error",
                    "message", "Session not found"
            ));
        }

        return session.updatesSink.asFlux();
    }

    /**
     * Відправити код
     */
    public void submitCode(String sessionId, String code) {
        AuthSession session = activeSessions.get(sessionId);
        if (session != null && session.codeFuture != null) {
            session.codeFuture.complete(code);
        }
    }

    /**
     * Відправити пароль
     */
    public void submitPassword(String sessionId, String password) {
        AuthSession session = activeSessions.get(sessionId);
        if (session != null && session.passwordFuture != null) {
            session.passwordFuture.complete(password);
        }
    }

    /**
     * Скасувати сесію
     */
    public void cancelAuthSession(String sessionId) {
        AuthSession session = activeSessions.remove(sessionId);
        if (session != null) {
            if (session.client != null) {
                try {
                    session.client.send(new TdApi.Close()).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Error closing client", e);
                }
            }
            if (session.factory != null) {
                try {
                    session.factory.close();
                } catch (Exception e) {
                    log.error("Error closing factory", e);
                }
            }
        }
    }

    /**
     * Статус сесії
     */
    public Map<String, Object> getSessionStatus(String sessionId) {
        AuthSession session = activeSessions.get(sessionId);
        if (session == null) {
            return Map.of("found", false);
        }

        return Map.of(
                "found", true,
                "status", session.status,
                "sessionId", sessionId
        );
    }

    /**
     * Ініціалізація Telegram клієнта
     */
    private void initializeTelegramClient(AuthSession session) {
        try {
            log.info("Initializing Telegram client for session: {}", session.sessionId);

            TDLibSettings settings = TDLibSettings.create(new APIToken(session.apiId, session.apiHash));
            settings.setDatabaseDirectoryPath(Paths.get("tdlib", "auth", session.sessionId));
            settings.setDownloadedFilesDirectoryPath(Paths.get("tdlib", "downloads", session.sessionId));

            SimpleTelegramClientFactory factory = new SimpleTelegramClientFactory();
            session.factory = factory;

            SimpleTelegramClientBuilder builder = factory.builder(settings);

            // Створюємо custom authentication data
            SimpleAuthenticationSupplier<?> authSupplier = createAuthSupplier(session);

            SimpleTelegramClient client = builder.build(authSupplier);
            session.client = client;

            // Слухаємо тільки потрібні updates
            client.addUpdatesHandler(update -> {
                // Фільтруємо тільки важливі events для аутентифікації
                if (update instanceof TdApi.UpdateAuthorizationState || update instanceof TdApi.UpdateUser) {
                    handleUpdate(session, update);
                }
                // Ігноруємо всі інші updates під час аутентифікації
            });

            sendUpdate(session, "status", Map.of("message", "Client initialized"));

        } catch (Exception e) {
            log.error("Error initializing Telegram client", e);
            sendUpdate(session, "error", Map.of("message", e.getMessage()));
            session.status = "error";
        }
    }

    /**
     * Створити authentication supplier
     */
    private SimpleAuthenticationSupplier<?> createAuthSupplier(AuthSession session) {
        if ("qrcode".equals(session.authMethod)) {
            return AuthenticationSupplier.qrCode();
        } else {
            return AuthenticationSupplier.user(session.phoneNumber);
        }
    }

    /**
     * Обробка updates від TDLight
     */
    private void handleUpdate(AuthSession session, Object update) {
        try {
            if (update instanceof TdApi.UpdateAuthorizationState authUpdate) {
                handleAuthorizationState(session, authUpdate.authorizationState);
            } else if (update instanceof TdApi.UpdateUser userUpdate) {
                // Зберігаємо дані користувача якщо це наш користувач
                if (userUpdate.user.id != 0 && session.status.equals("ready")) {
                    session.currentUser = userUpdate.user;
                    log.info("Received user data: {} {}", userUpdate.user.firstName, userUpdate.user.lastName);
                }
            } else if (update instanceof TdApi.Error error) {
                handleError(session, error);
            }
        } catch (Exception e) {
            log.error("Error handling update", e);
        }
    }

    /**
     * Обробка помилок від Telegram
     */
    private void handleError(AuthSession session, TdApi.Error error) {
        log.error("Telegram error: {} - {}", error.code, error.message);

        String userMessage = switch (error.code) {
            case 400 -> {
                if (error.message.contains("PASSWORD_HASH_INVALID")) {
                    yield "Incorrect password. Please try again.";
                } else if (error.message.contains("PHONE_CODE_INVALID")) {
                    yield "Invalid confirmation code. Please try again.";
                } else if (error.message.contains("PHONE_CODE_EXPIRED")) {
                    yield "Confirmation code expired. Please request a new one.";
                }
                yield "Invalid input: " + error.message;
            }
            case 401 -> "Unauthorized. Please check your credentials.";
            case 420 -> "Too many attempts. Please wait and try again later.";
            case 500 -> "Telegram server error. Please try again later.";
            default -> "Error: " + error.message;
        };

        sendUpdate(session, "error", Map.of(
                "message", userMessage,
                "code", error.code,
                "originalMessage", error.message
        ));

        // Якщо помилка пароля, дозволяємо спробувати знову
        if (error.code == 400 && error.message.contains("PASSWORD_HASH_INVALID")) {
            session.status = "waiting_password";
            session.passwordFuture = new CompletableFuture<>();

            sendUpdate(session, "password_required", Map.of(
                    "message", "Incorrect password. Please try again.",
                    "retry", true
            ));

            session.passwordFuture.thenAccept(password -> {
                session.client.send(new TdApi.CheckAuthenticationPassword(password));
            });
        }
    }

    /**
     * Обробка authorization state
     */
    private void handleAuthorizationState(AuthSession session, TdApi.AuthorizationState state) {
        log.info("Authorization state changed: {}", state.getClass().getSimpleName());

        if (state instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
            session.status = "waiting_tdlib_params";
            sendUpdate(session, "status", Map.of("message", "Initializing..."));
        }
        else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            session.status = "waiting_phone";
            sendUpdate(session, "phone_required", Map.of("message", "Phone number required"));

            // Якщо є номер, відправляємо автоматично
            if (session.phoneNumber != null && !session.phoneNumber.isEmpty()) {
                session.client.send(new TdApi.SetAuthenticationPhoneNumber(session.phoneNumber, null));
            }
        }
        else if (state instanceof TdApi.AuthorizationStateWaitOtherDeviceConfirmation qrState) {
            session.status = "waiting_qr";
            sendUpdate(session, "qr_code", Map.of(
                    "link", qrState.link,
                    "message", "Scan QR code in Telegram app"
            ));
        }
        else if (state instanceof TdApi.AuthorizationStateWaitCode codeInfo) {
            session.status = "waiting_code";
            session.codeFuture = new CompletableFuture<>();

            Map<String, Object> codeData = new HashMap<>();
            codeData.put("message", "Enter confirmation code");

            if (codeInfo.codeInfo != null) {
                codeData.put("type", codeInfo.codeInfo.type.getClass().getSimpleName());
                codeData.put("nextType", codeInfo.codeInfo.nextType != null ?
                        codeInfo.codeInfo.nextType.getClass().getSimpleName() : null);
                codeData.put("timeout", codeInfo.codeInfo.timeout);
            }

            sendUpdate(session, "code_required", codeData);

            // Чекаємо на код
            session.codeFuture.thenAccept(code -> {
                session.client.send(new TdApi.CheckAuthenticationCode(code));
            });
        }
        else if (state instanceof TdApi.AuthorizationStateWaitPassword passwordState) {
            session.status = "waiting_password";
            session.passwordFuture = new CompletableFuture<>();

            Map<String, Object> passwordData = new HashMap<>();
            passwordData.put("message", "Enter 2FA password");
            passwordData.put("hasRecoveryEmail", passwordState.hasRecoveryEmailAddress);
            passwordData.put("recoveryEmailPattern", passwordState.recoveryEmailAddressPattern);

            if (passwordState.passwordHint != null && !passwordState.passwordHint.isEmpty()) {
                passwordData.put("hint", passwordState.passwordHint);
            }

            sendUpdate(session, "password_required", passwordData);

            // Чекаємо на пароль
            session.passwordFuture.thenAccept(password -> {
                session.client.send(new TdApi.CheckAuthenticationPassword(password));
            });
        }
        else if (state instanceof TdApi.AuthorizationStateWaitRegistration) {
            session.status = "waiting_registration";
            sendUpdate(session, "registration_required", Map.of(
                    "message", "This phone number is not registered. Please register first."
            ));
        }
        else if (state instanceof TdApi.AuthorizationStateReady) {
            session.status = "ready";
            // Запускаємо в окремому потоці щоб не блокувати обробку updates
            CompletableFuture.runAsync(() -> handleAuthenticationComplete(session));
        }
        else if (state instanceof TdApi.AuthorizationStateClosed) {
            session.status = "closed";
            sendUpdate(session, "closed", Map.of("message", "Session closed"));
        }
        else if (state instanceof TdApi.AuthorizationStateClosing) {
            session.status = "closing";
            sendUpdate(session, "status", Map.of("message", "Closing session..."));
        }
        else if (state instanceof TdApi.AuthorizationStateLoggingOut) {
            session.status = "logging_out";
            sendUpdate(session, "status", Map.of("message", "Logging out..."));
        }
    }

    /**
     * Обробка успішної аутентифікації
     */
    private void handleAuthenticationComplete(AuthSession session) {
        try {
            log.info("Authentication completed for session: {}", session.sessionId);

            // Чекаємо трохи щоб клієнт повністю ініціалізувався
            Thread.sleep(500);

            // Спробуємо отримати дані користувача
            TdApi.User me = null;

            // Спочатку перевіряємо чи вже є дані з UpdateUser
            if (session.currentUser != null) {
                me = session.currentUser;
                log.info("Using user data from UpdateUser");
            } else {
                // Якщо немає, робимо запит з retry
                me = getUserWithRetry(session, 3);
            }

            // Зберігаємо credentials
            Map<String, Object> credentialData = new HashMap<>();
            credentialData.put("apiId", String.valueOf(session.apiId));
            credentialData.put("apiHash", session.apiHash);
            credentialData.put("phoneNumber", session.phoneNumber);
            credentialData.put("userId", me.id);
            credentialData.put("firstName", me.firstName);
            credentialData.put("lastName", me.lastName);
            credentialData.put("username", me.usernames != null && me.usernames.activeUsernames.length > 0 ?
                    me.usernames.activeUsernames[0] : "");

            String encryptedData = encryption.encrypt(serializeToJson(credentialData));

            Credential credential = Credential.builder()
                    .name("Telegram - " + me.firstName + " " + me.lastName)
                    .type(CredentialType.TELEGRAM_CLIENT)
                    .encryptedData(encryptedData)
                    .description("Authorized via " + session.authMethod)
                    .build();

            credential = credentialRepository.save(credential);

            sendUpdate(session, "success", Map.of(
                    "credentialId", credential.getId(),
                    "message", "Authentication successful",
                    "user", Map.of(
                            "id", me.id,
                            "firstName", me.firstName,
                            "lastName", me.lastName,
                            "username", credentialData.get("username")
                    )
            ));

            // Закриваємо сесію після успіху
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(2000);
                    cancelAuthSession(session.sessionId);
                } catch (Exception e) {
                    log.error("Error closing session", e);
                }
            });

        } catch (Exception e) {
            log.error("Error saving credentials", e);
            sendUpdate(session, "error", Map.of("message", "Failed to save credentials: " + e.getMessage()));
        }
    }

    /**
     * Відправити update на фронтенд
     */
    private void sendUpdate(AuthSession session, String type, Map<String, Object> data) {
        Map<String, Object> update = new HashMap<>(data);
        update.put("type", type);
        update.put("timestamp", System.currentTimeMillis());

        session.updatesSink.tryEmitNext(update);
    }

    private String serializeToJson(Map<String, Object> data) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            log.error("Error serializing to JSON", e);
            return "{}";
        }
    }

    /**
     * Отримати дані користувача з retry логікою
     */
    private TdApi.User getUserWithRetry(AuthSession session, int maxRetries) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Attempting to get user info, attempt {}/{}", attempt, maxRetries);

                CompletableFuture<TdApi.User> future = session.client.send(new TdApi.GetMe());
                TdApi.User user = future.get(15, TimeUnit.SECONDS);

                log.info("Successfully retrieved user info: {} {}", user.firstName, user.lastName);
                return user;

            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("Timeout getting user info on attempt {}/{}", attempt, maxRetries);
                lastException = e;

                if (attempt < maxRetries) {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                }
            } catch (Exception e) {
                log.error("Error getting user info on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                lastException = e;

                if (attempt < maxRetries) {
                    Thread.sleep(1000);
                }
            }
        }

        throw new Exception("Failed to get user info after " + maxRetries + " attempts", lastException);
    }

    /**
     * Клас для зберігання даних сесії
     */
    private static class AuthSession {
        String sessionId;
        int apiId;
        String apiHash;
        String phoneNumber;
        String authMethod;
        String status;
        SimpleTelegramClient client;
        SimpleTelegramClientFactory factory;
        CompletableFuture<String> codeFuture;
        CompletableFuture<String> passwordFuture;
        Sinks.Many<Map<String, Object>> updatesSink;
        TdApi.User currentUser; // Зберігаємо дані користувача
    }
}