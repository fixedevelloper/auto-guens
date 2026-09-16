package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(

        @NotNull(message = "operationType est obligatoire (DEPOSIT ou WITHDRAW)")
        OperationType operationType,

        @NotBlank(message = "phone est obligatoire")
        String phone,

        @NotNull(message = "operator est obligatoire")
        Operator operator,

        @NotNull(message = "amount est obligatoire")
        @DecimalMin(value = "0.01", message = "amount doit être positif")
        BigDecimal amount,

        @NotBlank(message = "senderName est obligatoire")
        String senderName,

        @NotBlank(message = "countryCode est obligatoire")
        String countryCode,

        String description
) {
}
