package com.ussdauto.api.web.dto;

import com.ussdauto.api.domain.enums.Operator;

/** Patch partiel — tous les champs sont optionnels, seuls les champs non-null sont appliqués. */
public record SimSlotUpdateRequest(
        Operator operator,
        String phoneNumberOnSim,
        Boolean actif
) {
}
