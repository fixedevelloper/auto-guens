package com.ussdauto.api.domain.entity;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.domain.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_transaction_statut", columnList = "statut"),
        @Index(name = "idx_transaction_operation_type", columnList = "operation_type"),
        @Index(name = "idx_transaction_operator", columnList = "operator"),
        @Index(name = "idx_transaction_created_at", columnList = "created_at"),
        @Index(name = "idx_transaction_device_id", columnList = "device_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operator operator;

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus statut = TransactionStatus.PENDING;

    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "sim_slot_used")
    private Integer simSlotUsed;

    @Column(name = "ussd_code_used")
    private String ussdCodeUsed;

    @Column(name = "sms_raw_content", columnDefinition = "TEXT")
    private String smsRawContent;

    @Column(name = "operator_reference", length = 100)
    private String operatorReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isInFinalState() {
        return statut.isFinal();
    }
}
