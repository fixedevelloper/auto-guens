package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.entity.DeviceSimSlot;
import com.ussdauto.api.exception.DeviceNotFoundException;
import com.ussdauto.api.exception.SimSlotNotFoundException;
import com.ussdauto.api.repository.DeviceRepository;
import com.ussdauto.api.repository.DeviceSimSlotRepository;
import com.ussdauto.api.web.dto.SimSlotItemRequest;
import com.ussdauto.api.web.dto.SimSlotUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gère la config des puces SIM d'un device. Toute modification déclenche une
 * notification FCM SIM_CONFIG_UPDATED vers le device concerné, pour resynchronisation
 * immédiate côté app en plus du GET fait au démarrage.
 */
@Service
@RequiredArgsConstructor
public class SimSlotService {

    private final DeviceRepository deviceRepository;
    private final DeviceSimSlotRepository deviceSimSlotRepository;
    private final FcmNotificationService fcmNotificationService;

    @Transactional
    public List<DeviceSimSlot> replaceAll(UUID deviceId, List<SimSlotItemRequest> items) {
        Device device = getDevice(deviceId);

        Map<Integer, DeviceSimSlot> existingBySlot = deviceSimSlotRepository
                .findByDeviceIdOrderBySlotIndexAsc(deviceId).stream()
                .collect(Collectors.toMap(DeviceSimSlot::getSlotIndex, slot -> slot));

        for (SimSlotItemRequest item : items) {
            DeviceSimSlot slot = existingBySlot.remove(item.slotIndex());
            if (slot == null) {
                slot = DeviceSimSlot.builder().deviceId(deviceId).slotIndex(item.slotIndex()).build();
            }
            slot.setOperator(item.operator());
            slot.setPhoneNumberOnSim(item.phoneNumberOnSim());
            slot.setActif(true);
            deviceSimSlotRepository.save(slot);
        }

        // Les slots omis de la requête sont désactivés plutôt que supprimés (conserve l'historique).
        existingBySlot.values().forEach(slot -> {
            slot.setActif(false);
            deviceSimSlotRepository.save(slot);
        });

        fcmNotificationService.sendSimConfigUpdated(device);
        return deviceSimSlotRepository.findByDeviceIdOrderBySlotIndexAsc(deviceId);
    }

    @Transactional(readOnly = true)
    public List<DeviceSimSlot> getByDevice(UUID deviceId) {
        getDevice(deviceId);
        return deviceSimSlotRepository.findByDeviceIdOrderBySlotIndexAsc(deviceId);
    }

    @Transactional
    public DeviceSimSlot updateOne(UUID deviceId, Integer slotIndex, SimSlotUpdateRequest request) {
        Device device = getDevice(deviceId);

        DeviceSimSlot slot = deviceSimSlotRepository.findByDeviceIdAndSlotIndex(deviceId, slotIndex)
                .orElseThrow(() -> new SimSlotNotFoundException(deviceId, slotIndex));

        if (request.operator() != null) {
            slot.setOperator(request.operator());
        }
        if (request.phoneNumberOnSim() != null) {
            slot.setPhoneNumberOnSim(request.phoneNumberOnSim());
        }
        if (request.actif() != null) {
            slot.setActif(request.actif());
        }

        DeviceSimSlot saved = deviceSimSlotRepository.save(slot);
        fcmNotificationService.sendSimConfigUpdated(device);
        return saved;
    }

    private Device getDevice(UUID deviceId) {
        return deviceRepository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }
}
