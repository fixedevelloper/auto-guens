package com.ussdauto.api.domain.entity;

import com.ussdauto.api.domain.enums.Operator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Config d'une puce SIM physique d'un device, gérée depuis l'API et synchronisée vers l'app. */
@Entity
@Table(
        name = "device_sim_slot",
        uniqueConstraints = @UniqueConstraint(name = "uq_device_slot", columnNames = {"device_id", "slot_index"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceSimSlot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operator operator;

    @Column(name = "phone_number_on_sim", length = 20)
    private String phoneNumberOnSim;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
