package org.ewgf.configuration;

import lombok.Getter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import org.postgresql.util.PSQLException;

import java.util.HashMap;
import java.util.Map;

@Getter
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${concurrency.rabbitmq}")
    private String rabbitConcurrency;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key")
    private String routingKey;

    private final VirtualThreadConfig virtualThreadConfig;

    public RabbitMQConfig(VirtualThreadConfig virtualThreadConfig) {
        this.virtualThreadConfig = virtualThreadConfig;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        // Set concurrent consumers based on property
        factory.setConcurrentConsumers(Integer.parseInt(rabbitConcurrency));
        factory.setMaxConcurrentConsumers(Integer.parseInt(rabbitConcurrency));

        // Set the task executor to use virtual threads
        factory.setTaskExecutor(virtualThreadConfig.rabbitVirtualThreadExecutor());

        factory.setRetryTemplate(createRetryTemplate());

        return factory;
    }

    private RetryTemplate createRetryTemplate()
    {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Custom retry policy that specifically handles deadlocks
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5, createRetryableExceptions());

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(100); // 100ms initial backoff
        backOffPolicy.setMultiplier(2.0); // Double the wait time for each retry
        backOffPolicy.setMaxInterval(2000); // Cap at 2 seconds

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.registerListener(new LoggingRetryListener());

        return retryTemplate;
    }

    private Map<Class<? extends Throwable>, Boolean> createRetryableExceptions()
    {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();

        retryableExceptions.put(PSQLException.class, true);
        retryableExceptions.put(CannotAcquireLockException.class, true);
        retryableExceptions.put(PessimisticLockingFailureException.class, true);

        return retryableExceptions;
    }


    @Bean
    public Queue queue() {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}

