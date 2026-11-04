package dev.noby.history.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PackageHistoryMessage(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        Long packageId,
        HistoryEventType type,
        String actorId,
        Map<String, Object> before,
        Map<String, Object> after) {
}
