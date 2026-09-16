package com.ussdauto.api.exception;

import com.ussdauto.api.domain.enums.TransactionStatus;

/** Levée quand le device tente de rapporter un statut piloté uniquement par l'API. */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(TransactionStatus status) {
        super("Le statut " + status + " ne peut pas être rapporté via le callback device");
    }
}
