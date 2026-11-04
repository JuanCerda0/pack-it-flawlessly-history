package dev.noby.history.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PackageHistoryResponse(
        UUID eventId,
        Long packageId,
        HistoryEventType type,
        Instant occurredAt,
        String actorId,
        Map<String, Object> before,
        Map<String, Object> after) {
}
