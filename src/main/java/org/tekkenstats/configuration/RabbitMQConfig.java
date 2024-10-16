package org.tekkenstats.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;

@Configuration
public class RabbitMQConfig {


    public static final String QUEUE_NAME = "battle_queue";
    public static final String EXCHANGE_NAME = "battle_exchange";
    public static final String ROUTING_KEY = "battle.routingkey";

    @Bean
    public Queue queue()
    {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public TopicExchange exchange()
    {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange)
    {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory)
    {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        // Set the task executor to use virtual threads
        factory.setTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());

        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Add the LoggingRetryListener
        retryTemplate.registerListener(new LoggingRetryListener());

        factory.setRetryTemplate(retryTemplate);

        return factory;
    }

    @Bean
    public MessageConverter messageConverter()
    {
        // Use the SimpleMessageConverter for plain text messages
        return new SimpleMessageConverter();
    }

        @Bean
        public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory)
        {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            return new RabbitAdmin(connectionFactory);

        }

    }

