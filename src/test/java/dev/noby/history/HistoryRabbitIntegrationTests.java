package dev.noby.history;

import dev.noby.history.repository.PackageHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "history.rabbit.integration", matches = "true")
class HistoryRabbitIntegrationTests {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PackageHistoryRepository repository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void receivesAndDeduplicatesRealRabbitMessages() throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        String payload = """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"%s","packageId":42,
                 "type":"PACKAGE_CREATED","actorId":null,"before":null,
                 "after":{"name":"Prueba RabbitMQ","status":"RECEIVED"}}
                """.formatted(eventId, Instant.now());
        Message message = new Message(payload.getBytes(StandardCharsets.UTF_8), new MessageProperties());

        try {
            rabbitTemplate.send("packit.package.events", "package.created", message);
            rabbitTemplate.send("packit.package.events", "package.created", message);

            for (int attempt = 0; attempt < 50 && !repository.existsById(eventId); attempt++) {
                Thread.sleep(100);
            }
            assertThat(repository.existsById(eventId)).isTrue();

            for (int attempt = 0; attempt < 50; attempt++) {
                Integer pending = rabbitTemplate.execute(channel ->
                        channel.queueDeclarePassive("history.package").getMessageCount());
                if (pending != null && pending == 0) {
                    break;
                }
                Thread.sleep(100);
            }
            assertThat(repository.findByPackageIdOrderByOccurredAtAscEventIdAsc(42L).stream()
                    .filter(event -> event.getEventId().equals(eventId))).hasSize(1);
        } finally {
            repository.deleteById(eventId);
        }
    }
}
