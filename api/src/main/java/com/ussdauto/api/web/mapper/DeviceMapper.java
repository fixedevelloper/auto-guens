package com.ussdauto.api.web.mapper;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.web.dto.DeviceResponse;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getApiKey(),
                device.getStatut(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
