package com.ussdauto.api.domain.enums;

import java.util.Set;

public enum TransactionStatus {
    PENDING,
    SENT_TO_DEVICE,
    RECEIVED_BY_DEVICE,
    EXECUTING,
    SUCCESS,
    FAILED,
    TIMEOUT;

    private static final Set<TransactionStatus> FINAL_STATUSES = Set.of(SUCCESS, FAILED, TIMEOUT);

    /** Statuts que le device est autorisé à rapporter via POST /transactions/{id}/status. */
    private static final Set<TransactionStatus> CALLBACK_ALLOWED = Set.of(
            RECEIVED_BY_DEVICE, EXECUTING, SUCCESS, FAILED
    );

    /** Statuts surveillés par le job de timeout (bloqués depuis trop longtemps). */
    private static final Set<TransactionStatus> TIMEOUT_ELIGIBLE = Set.of(SENT_TO_DEVICE, EXECUTING);

    public boolean isFinal() {
        return FINAL_STATUSES.contains(this);
    }

    public boolean isAllowedInCallback() {
        return CALLBACK_ALLOWED.contains(this);
    }

    public static Set<TransactionStatus> timeoutEligible() {
        return TIMEOUT_ELIGIBLE;
    }
}
