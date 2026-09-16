package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.entity.DeviceSimSlot;
import com.ussdauto.api.domain.enums.DeviceStatus;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.exception.NoDeviceAvailableException;
import com.ussdauto.api.repository.DeviceRepository;
import com.ussdauto.api.repository.DeviceSimSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceSelectionServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceSimSlotRepository deviceSimSlotRepository;

    @InjectMocks
    private DeviceSelectionService deviceSelectionService;

    @Test
    void selectsTheLeastRecentlySeenOnlineDeviceAndResolvesItsActiveSlotForTheOperator() {
        Device staleDevice = device("stale", Instant.parse("2024-01-01T00:00:00Z"));
        Device freshDevice = device("fresh", Instant.parse("2024-06-01T00:00:00Z"));

        when(deviceRepository.findOnlineWithActiveSlotForOperator(DeviceStatus.ONLINE, Operator.MTN))
                .thenReturn(List.of(freshDevice, staleDevice));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceSimSlot slot = DeviceSimSlot.builder()
                .deviceId(staleDevice.getId()).slotIndex(1).operator(Operator.MTN).actif(true).build();
        when(deviceSimSlotRepository.findFirstByDeviceIdAndOperatorAndActifTrue(staleDevice.getId(), Operator.MTN))
                .thenReturn(Optional.of(slot));

        DeviceAssignment assignment = deviceSelectionService.selectAndReserveDevice(Operator.MTN);

        assertThat(assignment.device().getApiKey()).isEqualTo("stale");
        assertThat(assignment.device().getStatut()).isEqualTo(DeviceStatus.BUSY);
        assertThat(assignment.simSlotIndex()).isEqualTo(1);
    }

    @Test
    void throwsWhenNoOnlineDeviceHasAnActiveSlotForTheOperator() {
        when(deviceRepository.findOnlineWithActiveSlotForOperator(DeviceStatus.ONLINE, Operator.ORANGE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> deviceSelectionService.selectAndReserveDevice(Operator.ORANGE))
                .isInstanceOf(NoDeviceAvailableException.class);
    }

    @Test
    void releaseSetsDeviceBackOnline() {
        UUID deviceId = UUID.randomUUID();
        Device busyDevice = device("busy", Instant.now());
        busyDevice.setId(deviceId);
        busyDevice.setStatut(DeviceStatus.BUSY);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(busyDevice));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deviceSelectionService.release(deviceId);

        assertThat(busyDevice.getStatut()).isEqualTo(DeviceStatus.ONLINE);
        verify(deviceRepository).save(busyDevice);
    }

    private Device device(String apiKey, Instant lastSeenAt) {
        return Device.builder()
                .id(UUID.randomUUID())
                .apiKey(apiKey)
                .statut(DeviceStatus.ONLINE)
                .fcmToken("token-" + apiKey)
                .lastSeenAt(lastSeenAt)
                .build();
    }
}
