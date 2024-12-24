package org.ewgf;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableScheduling
@EnableRabbit
@EnableRetry
@EnableJpaRepositories
@EnableAsync
@EnableWebSecurity
public class Main {
    public static void main(String[] args)
    {
        SpringApplication.run(Main.class, args);
    }
}