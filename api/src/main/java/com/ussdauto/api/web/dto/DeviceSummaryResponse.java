package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.DeviceStatus;

import java.time.Instant;
import java.util.UUID;

/** Sans apiKey ni fcmToken, contrairement à DeviceResponse — pour la liste, pas de raison
 * de réexposer des secrets à chaque chargement. */
public record DeviceSummaryResponse(
        UUID id,
        DeviceStatus statut,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt
) {
}
