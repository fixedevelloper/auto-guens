package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Utilisée pour les listes (réconciliation) — sans l'historique complet, pour rester légère. */
public record TransactionSummaryResponse(
        UUID id,
        OperationType operationType,
        String phone,
        Operator operator,
        String countryCode,
        BigDecimal amount,
        String senderName,
        TransactionStatus statut,
        String operatorReference,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
