package com.training.marketplace.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the inventory (stock.*) RabbitMQ topology so the stock consumers' queues exist.
 * The order/payment topology lives in {@link RabbitMQConfig}; the message converter is shared
 * from there. Queue/exchange/binding beans are auto-declared by Spring's RabbitAdmin at startup.
 */
@Configuration
@EnableConfigurationProperties(AlertEmailProperties.class)
public class StockRabbitConfig {

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(StockRabbitTopology.STOCK_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockUpdateQueue() {
        return QueueBuilder.durable(StockRabbitTopology.STOCK_UPDATE_QUEUE).build();
    }

    @Bean
    public Queue reorderSuggestionQueue() {
        return QueueBuilder.durable(StockRabbitTopology.REORDER_SUGGESTION_QUEUE).build();
    }

    @Bean
    public Queue emailAlertQueue() {
        return QueueBuilder.durable(StockRabbitTopology.EMAIL_ALERT_QUEUE).build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(StockRabbitTopology.AUDIT_QUEUE).build();
    }

    @Bean
    public Binding stockUpdateBinding(
            @Qualifier("stockUpdateQueue") Queue queue,
            @Qualifier("stockExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(StockRabbitTopology.MOVEMENT_COMPLETED_PATTERN);
    }

    @Bean
    public Binding reorderSuggestionBinding(
            @Qualifier("reorderSuggestionQueue") Queue queue,
            @Qualifier("stockExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(StockRabbitTopology.LOW_STOCK_ROUTING_KEY);
    }

    @Bean
    public Binding emailAlertBinding(
            @Qualifier("emailAlertQueue") Queue queue,
            @Qualifier("stockExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(StockRabbitTopology.EMAIL_ALERT_PATTERN);
    }

    @Bean
    public Binding auditBinding(
            @Qualifier("auditQueue") Queue queue,
            @Qualifier("stockExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(StockRabbitTopology.AUDIT_PATTERN);
    }
}
