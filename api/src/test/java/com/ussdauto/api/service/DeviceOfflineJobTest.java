package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.enums.DeviceStatus;
import com.ussdauto.api.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceOfflineJobTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceOfflineJob deviceOfflineJob;

    @Test
    void marksStaleOnlineDevicesOffline() {
        ReflectionTestUtils.setField(deviceOfflineJob, "heartbeatTimeoutSeconds", 120L);

        Device staleDevice = Device.builder()
                .id(UUID.randomUUID())
                .apiKey("device-key")
                .statut(DeviceStatus.ONLINE)
                .lastSeenAt(Instant.now().minusSeconds(300))
                .build();

        when(deviceRepository.findByStatutAndLastSeenAtBefore(eq(DeviceStatus.ONLINE), any(Instant.class)))
                .thenReturn(List.of(staleDevice));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deviceOfflineJob.markStaleDevicesOffline();

        assertThat(staleDevice.getStatut()).isEqualTo(DeviceStatus.OFFLINE);
        verify(deviceRepository).save(staleDevice);
    }

    @Test
    void doesNothingWhenNoDeviceIsStale() {
        ReflectionTestUtils.setField(deviceOfflineJob, "heartbeatTimeoutSeconds", 120L);

        when(deviceRepository.findByStatutAndLastSeenAtBefore(eq(DeviceStatus.ONLINE), any(Instant.class)))
                .thenReturn(List.of());

        deviceOfflineJob.markStaleDevicesOffline();

        verify(deviceRepository, never()).save(any());
    }
}
