package com.workflow.sociallabs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration для async trigger event processing
 */
@Configuration
@EnableAsync
public class AsyncEventConfiguration {

    /**
     * Executor для async event handling
     */
    @Bean(name = "eventExecutor")
    public java.util.concurrent.Executor eventExecutor() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2
        );

        executor.setThreadFactory(new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("event-handler-" + counter.incrementAndGet());
                thread.setDaemon(false);
                return thread;
            }
        });

        return executor;
    }
}
