package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.DeviceStatus;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String apiKey,
        DeviceStatus statut,
        Instant createdAt,
        Instant updatedAt
) {
}
