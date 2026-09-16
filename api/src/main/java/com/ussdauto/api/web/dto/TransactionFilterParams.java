package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.domain.enums.TransactionStatus;

import java.time.Instant;

public record TransactionFilterParams(
        OperationType operationType,
        Operator operator,
        TransactionStatus statut,
        Instant from,
        Instant to
) {
}
