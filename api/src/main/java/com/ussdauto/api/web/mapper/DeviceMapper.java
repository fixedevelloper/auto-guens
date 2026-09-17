package com.ussdauto.api.web.mapper;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.web.dto.DeviceResponse;
import com.ussdauto.api.web.dto.DeviceSummaryResponse;
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

    public DeviceSummaryResponse toSummary(Device device) {
        return new DeviceSummaryResponse(
                device.getId(),
                device.getStatut(),
                device.getLastSeenAt(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
