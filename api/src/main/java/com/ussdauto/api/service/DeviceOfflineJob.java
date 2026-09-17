package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.enums.DeviceStatus;
import com.ussdauto.api.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Bascule en OFFLINE les devices ONLINE dont le dernier heartbeat (mise à jour du token
 * FCM via PATCH /api/devices/{id}) date de plus de [heartbeat-timeout-seconds], pour ne
 * plus les proposer à la sélection. Un device BUSY (transaction en cours) n'est jamais
 * touché ici — il repasse ONLINE via DeviceSelectionService#release à la fin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceOfflineJob {

    private final DeviceRepository deviceRepository;

    @Value("${ussd-automation.device.heartbeat-timeout-seconds:120}")
    private long heartbeatTimeoutSeconds;

    @Scheduled(fixedDelayString = "${ussd-automation.device.offline-job-fixed-delay-ms:30000}")
    @Transactional
    public void markStaleDevicesOffline() {
        Instant cutoff = Instant.now().minusSeconds(heartbeatTimeoutSeconds);
        List<Device> staleDevices = deviceRepository.findByStatutAndLastSeenAtBefore(DeviceStatus.ONLINE, cutoff);

        for (Device device : staleDevices) {
            device.setStatut(DeviceStatus.OFFLINE);
            deviceRepository.save(device);
            log.warn("Device {} basculé en OFFLINE (pas de heartbeat depuis plus de {}s)", device.getId(), heartbeatTimeoutSeconds);
        }
    }
}
