package com.ussdauto.api.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUssdTemplateRequest(
        @NotBlank(message = "template est obligatoire") String template,
        Boolean actif
) {
}
