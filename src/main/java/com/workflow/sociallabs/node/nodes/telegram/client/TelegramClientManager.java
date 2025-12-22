package com.workflow.sociallabs.node.nodes.telegram.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.sociallabs.domain.entity.Credential;
import com.workflow.sociallabs.domain.repository.CredentialRepository;
import com.workflow.sociallabs.security.CredentialEncryption;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Централізований менеджер Telegram клієнтів
 * Керує пулом клієнтів, їх життєвим циклом та переподключенням
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientManager {

    private final CredentialRepository credentialRepository;
    private final CredentialEncryption encryption;
    private final ObjectMapper objectMapper;

    // Пул активних клієнтів: credentialId -> ClientWrapper
    private final Map<Long, ClientWrapper> activeClients = new ConcurrentHashMap<>();

    /**
     * Отримати або створити клієнта для credential
     */
    public SimpleTelegramClient getOrCreateClient(Long credentialId) throws Exception {
        ClientWrapper wrapper = activeClients.get(credentialId);

        // Якщо клієнт існує і активний
        if (wrapper != null && wrapper.isActive()) {
            log.debug("Reusing existing client for credential: {}", credentialId);
            return wrapper.client;
        }

        // Створити новий клієнт
        synchronized (this) {
            // Double-check
            wrapper = activeClients.get(credentialId);
            if (wrapper != null && wrapper.isActive()) {
                return wrapper.client;
            }

            log.info("Creating new Telegram client for credential: {}", credentialId);
            wrapper = createNewClient(credentialId);
            activeClients.put(credentialId, wrapper);
            return wrapper.client;
        }
    }

    /**
     * Перевірити чи клієнт активний
     */
    public boolean isClientActive(Long credentialId) {
        ClientWrapper wrapper = activeClients.get(credentialId);
        return wrapper != null && wrapper.isActive();
    }

    /**
     * Закрити клієнта
     */
    public void closeClient(Long credentialId) {
        ClientWrapper wrapper = activeClients.remove(credentialId);
        if (wrapper != null) {
            wrapper.close();
        }
    }

    /**
     * Створити новий клієнт
     */
    private ClientWrapper createNewClient(Long credentialId) throws Exception {
        // Завантажити credentials з БД
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found: " + credentialId));

        // Розшифрувати дані
        String decryptedJson = encryption.decrypt(credential.getEncryptedData());
        Map<String, Object> credData = objectMapper.readValue(decryptedJson, Map.class);

        String apiId = (String) credData.get("apiId");
        String apiHash = (String) credData.get("apiHash");
        String phoneNumber = (String) credData.get("phoneNumber");

        // Налаштування TDLib
        TDLibSettings settings = TDLibSettings.create(
                new APIToken(Integer.parseInt(apiId), apiHash)
        );

        // Використовуємо унікальну директорію для кожного credential
        String sessionPath = "tdlib/sessions/credential_" + credentialId;
        settings.setDatabaseDirectoryPath(Paths.get(sessionPath, "data"));
        settings.setDownloadedFilesDirectoryPath(Paths.get(sessionPath, "downloads"));

        // Створити factory
        SimpleTelegramClientFactory factory = new SimpleTelegramClientFactory();

        // Створити authentication supplier
        // Оскільки користувач вже авторизований, використовуємо збережену сесію
        SimpleAuthenticationSupplier<?> authSupplier = AuthenticationSupplier.user(phoneNumber);

        // Створити клієнта
        SimpleTelegramClient client = factory.builder(settings).build(authSupplier);

        // Перевірити що клієнт готовий
        waitForClientReady(client);

        log.info("Telegram client created successfully for credential: {}", credentialId);

        return new ClientWrapper(client, factory, credentialId);
    }

    /**
     * Чекати поки клієнт стане готовим
     */
    private void waitForClientReady(SimpleTelegramClient client) throws Exception {
        // Спробувати отримати інфо про користувача
        TdApi.User me = client.send(new TdApi.GetMe()).get(30, TimeUnit.SECONDS);
        log.info("Client ready. User: {} {} (id: {})", me.firstName, me.lastName, me.id);
    }

    /**
     * Закрити всі клієнти при shutdown
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down all Telegram clients...");
        activeClients.values().forEach(ClientWrapper::close);
        activeClients.clear();
    }

    /**
     * Обгортка для клієнта з метаданими
     */
    private static class ClientWrapper {
        final SimpleTelegramClient client;
        final SimpleTelegramClientFactory factory;
        final Long credentialId;
        final long createdAt;
        volatile boolean active;

        ClientWrapper(SimpleTelegramClient client, SimpleTelegramClientFactory factory, Long credentialId) {
            this.client = client;
            this.factory = factory;
            this.credentialId = credentialId;
            this.createdAt = System.currentTimeMillis();
            this.active = true;
        }

        boolean isActive() {
            return active;
        }

        void close() {
            if (!active) return;

            active = false;
            log.info("Closing Telegram client for credential: {}", credentialId);

            try {
                client.send(new TdApi.Close()).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error closing client", e);
            }

            try {
                factory.close();
            } catch (Exception e) {
                log.error("Error closing factory", e);
            }
        }
    }
}