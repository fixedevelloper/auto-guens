package com.ussdauto.api.exception;

import java.util.UUID;

public class DeviceAuthMismatchException extends RuntimeException {

    public DeviceAuthMismatchException(UUID deviceId) {
        super("La clé API fournie ne correspond pas au device " + deviceId);
    }
}
