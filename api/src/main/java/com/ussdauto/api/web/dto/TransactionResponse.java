package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        OperationType operationType,
        String phone,
        Operator operator,
        String countryCode,
        BigDecimal amount,
        String senderName,
        String description,
        TransactionStatus statut,
        String deviceId,
        Integer simSlotUsed,
        String ussdCodeUsed,
        String operatorReference,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        List<StatusHistoryEntryResponse> statusHistory
) {
}
