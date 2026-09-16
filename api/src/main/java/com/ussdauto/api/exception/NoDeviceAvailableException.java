package com.ussdauto.api.exception;

import com.ussdauto.api.domain.enums.Operator;

public class NoDeviceAvailableException extends RuntimeException {

    public NoDeviceAvailableException(Operator operator) {
        super("Aucun device ONLINE avec une puce SIM active disponible pour l'opérateur " + operator);
    }
}
