package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;

import java.time.Instant;
import java.util.UUID;

public record SimSlotResponse(
        UUID id,
        UUID deviceId,
        Integer slotIndex,
        Operator operator,
        String phoneNumberOnSim,
        boolean actif,
        Instant updatedAt
) {
}
