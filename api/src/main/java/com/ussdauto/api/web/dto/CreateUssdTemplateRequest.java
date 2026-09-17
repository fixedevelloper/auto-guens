package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUssdTemplateRequest(
        @NotNull(message = "operator est obligatoire") Operator operator,
        @NotNull(message = "operationType est obligatoire") OperationType operationType,
        @NotBlank(message = "countryCode est obligatoire") String countryCode,
        @NotBlank(message = "template est obligatoire") String template
) {
}
