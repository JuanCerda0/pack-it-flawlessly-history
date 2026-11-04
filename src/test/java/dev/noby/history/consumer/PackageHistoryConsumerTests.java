package dev.noby.history.consumer;

import com.rabbitmq.client.Channel;
import dev.noby.history.dto.HistoryEventType;
import dev.noby.history.dto.PackageHistoryMessage;
import dev.noby.history.exception.InvalidHistoryMessageException;
import dev.noby.history.service.PackageHistoryService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataAccessResourceFailureException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PackageHistoryConsumerTests {

    private final PackageHistoryService service = mock(PackageHistoryService.class);
    private final PackageHistoryConsumer consumer = new PackageHistoryConsumer(service);
    private final Channel channel = mock(Channel.class);

    @Test
    void acknowledgesOnlyAfterHistoryIsStored() throws Exception {
        Message message = delivery();
        PackageHistoryMessage event = event();
        when(service.parse(message.getBody())).thenReturn(event);

        consumer.onMessage(message, channel);

        InOrder order = inOrder(service, channel);
        order.verify(service).record(event);
        order.verify(channel).basicAck(17L, false);
    }

    @Test
    void rejectsInvalidMessageWithoutRetry() throws Exception {
        Message message = delivery();
        when(service.parse(message.getBody())).thenThrow(new InvalidHistoryMessageException("versión inválida"));

        consumer.onMessage(message, channel);

        verify(service, times(1)).parse(message.getBody());
        verify(channel).basicReject(17L, false);
    }

    @Test
    void sendsPersistentDatabaseFailureToDlqAfterThreeAttempts() throws Exception {
        Message message = delivery();
        PackageHistoryMessage event = event();
        when(service.parse(message.getBody())).thenReturn(event);
        doThrow(new DataAccessResourceFailureException("postgres unavailable"))
                .when(service).record(event);

        consumer.onMessage(message, channel);

        verify(service, times(3)).record(event);
        verify(channel).basicReject(17L, false);
    }

    private Message delivery() {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(17L);
        return new Message("{}".getBytes(StandardCharsets.UTF_8), properties);
    }

    private PackageHistoryMessage event() {
        return new PackageHistoryMessage(UUID.randomUUID(), 1, Instant.now(), 42L,
                HistoryEventType.PACKAGE_CREATED, null, null,
                Map.of("name", "Paquete", "status", "RECEIVED"));
    }
}
