package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;

import java.time.Instant;
import java.util.UUID;

public record UssdTemplateResponse(
        UUID id,
        Operator operator,
        OperationType operationType,
        String template,
        boolean actif,
        Instant createdAt,
        Instant updatedAt
) {
}
