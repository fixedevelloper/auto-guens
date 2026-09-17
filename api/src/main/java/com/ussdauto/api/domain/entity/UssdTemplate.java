package com.ussdauto.api.domain.entity;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Un seul template ACTIF par (operator, operationType, countryCode) — appliqué en base
 * via un index unique partiel (voir db/schema-extra.sql), pas via une contrainte JPA
 * classique, pour pouvoir conserver l'historique des versions désactivées. Un même
 * operator (ex. MTN) a un code USSD marchand différent selon le pays.
 */
@Entity
@Table(name = "ussd_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UssdTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operator operator;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    /** ISO 3166-1 alpha-2, ex: "CM" — même convention que Transaction.countryCode. */
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    /** Ex: "*126*1*{amount}*{phone}#" */
    @Column(nullable = false)
    private String template;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public String resolve(String amount, String phone) {
        return template
                .replace("{amount}", amount)
                .replace("{phone}", phone);
    }
}
