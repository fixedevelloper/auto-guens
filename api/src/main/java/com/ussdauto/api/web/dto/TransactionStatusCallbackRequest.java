package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record TransactionStatusCallbackRequest(

        @NotNull(message = "status est obligatoire")
        TransactionStatus status,

        @NotNull(message = "timestamp est obligatoire")
        Instant timestamp,

        String rawSmsContent,

        String operatorReference,

        String failureReason
) {
}
