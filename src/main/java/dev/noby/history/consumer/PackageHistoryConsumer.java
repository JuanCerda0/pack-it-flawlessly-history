package dev.noby.history.consumer;

import com.rabbitmq.client.Channel;
import dev.noby.history.config.RabbitHistoryConfig;
import dev.noby.history.dto.PackageHistoryMessage;
import dev.noby.history.exception.InvalidHistoryMessageException;
import dev.noby.history.service.PackageHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.io.IOException;

@Component
public class PackageHistoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(PackageHistoryConsumer.class);

    private final PackageHistoryService historyService;

    public PackageHistoryConsumer(PackageHistoryService historyService) {
        this.historyService = historyService;
    }

    @RabbitListener(queues = RabbitHistoryConfig.HISTORY_QUEUE, ackMode = "MANUAL")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        PackageHistoryMessage event;
        try {
            event = historyService.parse(message.getBody());
        } catch (InvalidHistoryMessageException error) {
            log.warn("Invalid history message sent to DLQ: {}", error.getMessage());
            channel.basicReject(tag, false);
            return;
        }

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                historyService.record(event);
                channel.basicAck(tag, false);
                return;
            } catch (DataAccessException | TransactionException error) {
                log.warn("History event {} failed on attempt {}: {}", event.eventId(), attempt,
                        error.getMessage());
                if (attempt == 3) {
                    channel.basicReject(tag, false);
                    return;
                }
                try {
                    Thread.sleep(250L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    channel.basicNack(tag, false, true);
                    return;
                }
            } catch (RuntimeException error) {
                log.error("Unexpected failure for history event {}; sending to DLQ", event.eventId(), error);
                channel.basicReject(tag, false);
                return;
            }
        }
    }
}
