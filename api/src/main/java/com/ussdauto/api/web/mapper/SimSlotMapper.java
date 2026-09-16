package com.ussdauto.api.web.mapper;

import com.ussdauto.api.domain.entity.DeviceSimSlot;
import com.ussdauto.api.web.dto.SimSlotResponse;
import org.springframework.stereotype.Component;

@Component
public class SimSlotMapper {

    public SimSlotResponse toResponse(DeviceSimSlot slot) {
        return new SimSlotResponse(
                slot.getId(),
                slot.getDeviceId(),
                slot.getSlotIndex(),
                slot.getOperator(),
                slot.getPhoneNumberOnSim(),
                slot.isActif(),
                slot.getUpdatedAt()
        );
    }
}
