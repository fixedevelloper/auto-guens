package com.ussdauto.api.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDeviceFcmTokenRequest(
        @NotBlank(message = "fcmToken est obligatoire") String fcmToken
) {
}
