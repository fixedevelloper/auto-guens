package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Device;

/** Résultat de la sélection d'un device : l'appareil réservé + la puce SIM à utiliser. */
public record DeviceAssignment(Device device, int simSlotIndex) {
}
