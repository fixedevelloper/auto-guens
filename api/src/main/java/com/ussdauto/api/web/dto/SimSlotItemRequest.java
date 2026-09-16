package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SimSlotItemRequest(
        @NotNull(message = "slotIndex est obligatoire") @PositiveOrZero Integer slotIndex,
        @NotNull(message = "operator est obligatoire") Operator operator,
        String phoneNumberOnSim
) {
}
