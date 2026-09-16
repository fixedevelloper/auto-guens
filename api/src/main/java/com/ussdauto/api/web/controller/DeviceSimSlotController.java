package com.ussdauto.api.web.controller;

import com.ussdauto.api.service.SimSlotService;
import com.ussdauto.api.web.dto.SimSlotItemRequest;
import com.ussdauto.api.web.dto.SimSlotResponse;
import com.ussdauto.api.web.dto.SimSlotUpdateRequest;
import com.ussdauto.api.web.mapper.SimSlotMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices/{deviceId}/sim-slots")
@RequiredArgsConstructor
public class DeviceSimSlotController {

    private final SimSlotService simSlotService;
    private final SimSlotMapper simSlotMapper;

    @PostMapping
    public List<SimSlotResponse> replaceAll(@PathVariable UUID deviceId, @Valid @RequestBody List<SimSlotItemRequest> items) {
        return simSlotService.replaceAll(deviceId, items).stream().map(simSlotMapper::toResponse).toList();
    }

    @GetMapping
    public List<SimSlotResponse> list(@PathVariable UUID deviceId) {
        return simSlotService.getByDevice(deviceId).stream().map(simSlotMapper::toResponse).toList();
    }

    @PutMapping("/{slotIndex}")
    public SimSlotResponse updateOne(
            @PathVariable UUID deviceId,
            @PathVariable Integer slotIndex,
            @RequestBody SimSlotUpdateRequest request
    ) {
        return simSlotMapper.toResponse(simSlotService.updateOne(deviceId, slotIndex, request));
    }
}
