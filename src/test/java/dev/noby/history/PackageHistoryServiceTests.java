package dev.noby.history;

import dev.noby.history.dto.PackageHistoryMessage;
import dev.noby.history.dto.PackageHistoryResponse;
import dev.noby.history.exception.InvalidHistoryMessageException;
import dev.noby.history.service.PackageHistoryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@Transactional
class PackageHistoryServiceTests {

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    PackageHistoryService historyService;

    @Autowired
    EntityManager entityManager;

    @Test
    void storesEventOnceAndReturnsTimeline() {
        UUID eventId = UUID.randomUUID();
        String json = """
                {"eventId":"%s","schemaVersion":1,"occurredAt":"2026-11-04T12:00:00Z",
                 "packageId":42,"type":"PACKAGE_STATUS_CHANGED","actorId":"user-sub",
                 "before":{"status":"RECEIVED"},"after":{"status":"PREPARING"}}
                """.formatted(eventId);

        PackageHistoryMessage message = historyService.parse(json.getBytes(StandardCharsets.UTF_8));
        historyService.record(message);
        historyService.record(message);
        entityManager.flush();
        entityManager.clear();

        List<PackageHistoryResponse> timeline = historyService.getHistory(42L);
        assertEquals(1, timeline.size());
        assertEquals(eventId, timeline.getFirst().eventId());
        assertEquals("user-sub", timeline.getFirst().actorId());
        assertEquals("RECEIVED", timeline.getFirst().before().get("status"));
        assertEquals("PREPARING", timeline.getFirst().after().get("status"));
        assertEquals(List.of(), historyService.getHistory(43L));
    }

    @Test
    void rejectsUnknownSchemaVersion() {
        String json = """
                {"eventId":"%s","schemaVersion":2,"occurredAt":"2026-11-04T12:00:00Z",
                 "packageId":42,"type":"PACKAGE_CREATED","actorId":null,
                 "before":null,"after":{"name":"Paquete","status":"RECEIVED"}}
                """.formatted(UUID.randomUUID());
        assertThrows(InvalidHistoryMessageException.class,
                () -> historyService.parse(json.getBytes(StandardCharsets.UTF_8)));
    }
}
