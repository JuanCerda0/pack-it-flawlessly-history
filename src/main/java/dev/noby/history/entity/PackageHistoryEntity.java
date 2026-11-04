package dev.noby.history.entity;

import dev.noby.history.dto.HistoryEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "package_history")
public class PackageHistoryEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private HistoryEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", length = 255)
    private String actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private Map<String, Object> beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> afterData;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected PackageHistoryEntity() {
    }

    public PackageHistoryEntity(UUID eventId, Long packageId, HistoryEventType eventType, Instant occurredAt,
                                String actorId, Map<String, Object> beforeData, Map<String, Object> afterData) {
        this.eventId = eventId;
        this.packageId = packageId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.receivedAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public Long getPackageId() { return packageId; }
    public HistoryEventType getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActorId() { return actorId; }
    public Map<String, Object> getBeforeData() { return beforeData; }
    public Map<String, Object> getAfterData() { return afterData; }
    public Instant getReceivedAt() { return receivedAt; }
}
