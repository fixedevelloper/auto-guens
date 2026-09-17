package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.enums.DeviceStatus;
import com.ussdauto.api.exception.DeviceAuthMismatchException;
import com.ussdauto.api.exception.DeviceNotFoundException;
import com.ussdauto.api.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Provisionne les devices Android : aucun self-service, un marchand crée le device puis
 * configure l'app avec l'id et la clé API retournés (visibles uniquement à la création). */
@Service
@RequiredArgsConstructor
public class DeviceService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DeviceRepository deviceRepository;

    @Transactional
    public Device create() {
        Device device = Device.builder()
                .apiKey(generateApiKey())
                .statut(DeviceStatus.OFFLINE)
                .build();

        return deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public List<Device> list() {
        return deviceRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Device getById(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    /** Appelé par le device lui-même (X-Device-Api-Key) pour (ré)enregistrer son token FCM
     * courant — après une installation, une réinstallation ou une rotation du token par
     * Firebase. Fait passer le device OFFLINE → ONLINE ; ne touche pas à un device BUSY
     * (transaction en cours). */
    @Transactional
    public Device updateFcmToken(UUID id, String fcmToken, String callingDeviceApiKey) {
        Device device = deviceRepository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));

        if (!device.getApiKey().equals(callingDeviceApiKey)) {
            throw new DeviceAuthMismatchException(id);
        }

        device.setFcmToken(fcmToken);
        device.setLastSeenAt(Instant.now());
        if (device.getStatut() == DeviceStatus.OFFLINE) {
            device.setStatut(DeviceStatus.ONLINE);
        }

        return deviceRepository.save(device);
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
