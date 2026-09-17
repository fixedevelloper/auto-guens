package com.ussdauto.api.web.controller;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.service.DeviceService;
import com.ussdauto.api.web.dto.DeviceResponse;
import com.ussdauto.api.web.dto.UpdateDeviceFcmTokenRequest;
import com.ussdauto.api.web.mapper.DeviceMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceMapper deviceMapper;

    @PostMapping
    public ResponseEntity<DeviceResponse> create() {
        Device device = deviceService.create();
        DeviceResponse response = deviceMapper.toResponse(device);
        return ResponseEntity.created(URI.create("/api/devices/" + device.getId())).body(response);
    }

    @PatchMapping("/{id}")
    public DeviceResponse updateFcmToken(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeviceFcmTokenRequest request,
            Authentication authentication
    ) {
        String callingDeviceApiKey = (String) authentication.getPrincipal();
        Device device = deviceService.updateFcmToken(id, request.fcmToken(), callingDeviceApiKey);
        return deviceMapper.toResponse(device);
    }
}
