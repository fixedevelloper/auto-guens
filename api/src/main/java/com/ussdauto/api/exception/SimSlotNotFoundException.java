package com.ussdauto.api.exception;

import java.util.UUID;

public class SimSlotNotFoundException extends RuntimeException {

    public SimSlotNotFoundException(UUID deviceId, Integer slotIndex) {
        super("Slot SIM " + slotIndex + " introuvable pour le device " + deviceId);
    }
}
