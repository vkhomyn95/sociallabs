package com.workflow.sociallabs.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.util.concurrent.TimeUnit;

/**
 * Конфігурація WebClient для HTTP Request ноди.
 *
 * <p>Реєструється як Spring Bean.
 * ignoreSsl і proxy задаються per-request через декоратор,
 * тут налаштовуємо базові defaults.
 */
@Configuration
public class WebClientConfiguration {

    /** Максимальний розмір відповіді (default 10 MB) */
    @Value("${http.node.max-response-size-mb:10}")
    private int maxResponseSizeMb;

    @Bean
    public WebClient httpNodeWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))
                );

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs()
                        .maxInMemorySize(maxResponseSizeMb * 1024 * 1024))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * WebClient з вимкненою перевіркою SSL сертифікатів.
     * Використовується тільки якщо ignoreSslIssues = true.
     */
    @Bean("insecureHttpNodeWebClient")
    public WebClient insecureHttpNodeWebClient() throws Exception {
        var sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .secure(spec -> spec.sslContext(sslContext));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs()
                        .maxInMemorySize(maxResponseSizeMb * 1024 * 1024))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}

