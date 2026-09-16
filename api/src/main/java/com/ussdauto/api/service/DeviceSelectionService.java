package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.entity.DeviceSimSlot;
import com.ussdauto.api.domain.enums.DeviceStatus;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.exception.NoDeviceAvailableException;
import com.ussdauto.api.repository.DeviceRepository;
import com.ussdauto.api.repository.DeviceSimSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.UUID;

/**
 * Sélectionne un device ONLINE possédant une puce SIM active pour l'opérateur demandé,
 * résout le slotIndex correspondant, et réserve le device (BUSY) de façon atomique.
 */
@Service
@RequiredArgsConstructor
public class DeviceSelectionService {

    private final DeviceRepository deviceRepository;
    private final DeviceSimSlotRepository deviceSimSlotRepository;

    @Transactional
    public DeviceAssignment selectAndReserveDevice(Operator operator) {
        Device device = deviceRepository.findOnlineWithActiveSlotForOperator(DeviceStatus.ONLINE, operator)
                .stream()
                .min(Comparator.comparing(Device::getLastSeenAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new NoDeviceAvailableException(operator));

        DeviceSimSlot slot = deviceSimSlotRepository
                .findFirstByDeviceIdAndOperatorAndActifTrue(device.getId(), operator)
                .orElseThrow(() -> new NoDeviceAvailableException(operator));

        device.setStatut(DeviceStatus.BUSY);
        deviceRepository.save(device);

        return new DeviceAssignment(device, slot.getSlotIndex());
    }

    @Transactional
    public void release(UUID deviceId) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setStatut(DeviceStatus.ONLINE);
            deviceRepository.save(device);
        });
    }
}
