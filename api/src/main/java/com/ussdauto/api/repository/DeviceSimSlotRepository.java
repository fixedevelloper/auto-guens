package com.ussdauto.api.repository;

import com.ussdauto.api.domain.entity.DeviceSimSlot;
import com.ussdauto.api.domain.enums.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceSimSlotRepository extends JpaRepository<DeviceSimSlot, UUID> {

    List<DeviceSimSlot> findByDeviceIdOrderBySlotIndexAsc(UUID deviceId);

    Optional<DeviceSimSlot> findByDeviceIdAndSlotIndex(UUID deviceId, Integer slotIndex);

    Optional<DeviceSimSlot> findFirstByDeviceIdAndOperatorAndActifTrue(UUID deviceId, Operator operator);
}
