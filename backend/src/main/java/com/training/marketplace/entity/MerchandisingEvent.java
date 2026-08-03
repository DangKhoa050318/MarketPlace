package com.training.marketplace.entity;

import com.training.marketplace.enums.MerchandisingEventType;
import com.training.marketplace.enums.MerchandisingTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only impression/click log (B-407/408). {@code eventId} is a client-supplied UUID with a
 * UNIQUE constraint, giving idempotent dedup. {@code orderId} is set when the event is attributed to
 * a purchase (D-5). Not a {@link BaseEntity} — events are never updated.
 */
@Entity
@Table(name = "merchandising_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchandisingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private MerchandisingEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 12)
    private MerchandisingTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    void onPersist() {
        LocalDateTime now = LocalDateTime.now();
        if (occurredAt == null) {
            occurredAt = now;
        }
        if (receivedAt == null) {
            receivedAt = now;
        }
    }
}
