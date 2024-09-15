package org.tekkenstats.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public MessageConverter messageConverter()
    {
        // Use the SimpleMessageConverter for plain text messages
        return new SimpleMessageConverter();
    }
}
