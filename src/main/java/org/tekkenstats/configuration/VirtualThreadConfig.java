package org.tekkenstats.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class VirtualThreadConfig
{
    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadConfig.class);

    @Bean(name = "rabbitThreadExecutor")
    public ExecutorService rabbitVirtualThreadExecutor() {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name(" rabbit-thread: ", 0)
                .factory();

        return Executors.newThreadPerTaskExecutor(threadFactory);
    }

    @Bean(name = "statisticsThreadExecutor")
    public Executor statisticsVirtualThreadExecutor()
    {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name("statistics-thread: ", 1)
                .factory();

        return Executors.newThreadPerTaskExecutor(threadFactory);
    }


}

