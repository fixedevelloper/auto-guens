package com.ussdauto.api.domain.entity;

import com.ussdauto.api.domain.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only : une ligne par changement de statut rapporté par le device (ou par
 * l'API pour PENDING/SENT_TO_DEVICE/TIMEOUT). Jamais mise à jour ni supprimée.
 */
@Entity
@Table(name = "transaction_status_history", indexes = {
        @Index(name = "idx_status_history_transaction", columnList = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionStatusHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus statut;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "sms_raw_content", columnDefinition = "TEXT")
    private String rawSmsContent;

    @Column(name = "operator_reference", length = 100)
    private String operatorReference;

    @Column(name = "failure_reason")
    private String failureReason;

    /** JSON brut du body reçu (audit), null pour les transitions pilotées par l'API elle-même. */
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
