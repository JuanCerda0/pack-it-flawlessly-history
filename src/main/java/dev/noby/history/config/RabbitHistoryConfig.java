package dev.noby.history.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableRabbit
public class RabbitHistoryConfig {

    public static final String HISTORY_QUEUE = "history.package";

    @Bean
    Declarables historyTopology() {
        TopicExchange events = new TopicExchange("packit.package.events", true, false);
        DirectExchange deadLetters = new DirectExchange("packit.dead.letters", true, false);
        Queue history = QueueBuilder.durable(HISTORY_QUEUE)
                .withArgument("x-queue-type", "classic")
                .deadLetterExchange(deadLetters.getName())
                .deadLetterRoutingKey(HISTORY_QUEUE + ".dlq")
                .build();
        Queue dlq = QueueBuilder.durable(HISTORY_QUEUE + ".dlq")
                .withArgument("x-queue-type", "classic")
                .build();

        return new Declarables(List.of(events, deadLetters, history, dlq,
                BindingBuilder.bind(history).to(events).with("package.#"),
                BindingBuilder.bind(dlq).to(deadLetters).with(dlq.getName())));
    }
}
