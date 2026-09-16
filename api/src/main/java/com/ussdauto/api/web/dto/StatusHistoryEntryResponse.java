package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.TransactionStatus;

import java.time.Instant;

public record StatusHistoryEntryResponse(
        TransactionStatus statut,
        Instant receivedAt,
        String rawSmsContent,
        String operatorReference,
        String failureReason
) {
}
