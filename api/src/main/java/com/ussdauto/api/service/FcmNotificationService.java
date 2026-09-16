package com.ussdauto.api.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Envoie les commandes au device Android via FCM (messages data-only, traités par
 * FirebaseMessagingService même app en arrière-plan) : commande d'exécution de
 * transaction, et notification de mise à jour de la config SIM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private static final String MESSAGE_TYPE_KEY = "messageType";
    private static final String MESSAGE_TYPE_COMMAND = "TRANSACTION_COMMAND";
    private static final String MESSAGE_TYPE_SIM_CONFIG_UPDATED = "SIM_CONFIG_UPDATED";

    private final FirebaseMessaging firebaseMessaging;

    public void sendCommand(Device device, Transaction transaction, String ussdCode, int simSlotIndex) {
        Message message = Message.builder()
                .setToken(device.getFcmToken())
                .putData(MESSAGE_TYPE_KEY, MESSAGE_TYPE_COMMAND)
                .putData("transactionId", transaction.getId().toString())
                .putData("operationType", transaction.getOperationType().name())
                .putData("phone", transaction.getPhone())
                .putData("operator", transaction.getOperator().name())
                .putData("amount", transaction.getAmount().toPlainString())
                .putData("senderName", transaction.getSenderName())
                .putData("countryCode", transaction.getCountryCode())
                .putData("description", transaction.getDescription() == null ? "" : transaction.getDescription())
                .putData("simSlotIndex", String.valueOf(simSlotIndex))
                .putData("ussdCode", ussdCode)
                .build();

        send(message, "commande de transaction " + transaction.getId());
    }

    public void sendSimConfigUpdated(Device device) {
        Message message = Message.builder()
                .setToken(device.getFcmToken())
                .putData(MESSAGE_TYPE_KEY, MESSAGE_TYPE_SIM_CONFIG_UPDATED)
                .build();

        send(message, "notification SIM_CONFIG_UPDATED pour device " + device.getId());
    }

    private void send(Message message, String description) {
        try {
            String messageId = firebaseMessaging.send(message);
            log.info("FCM envoyé ({}), messageId={}", description, messageId);
        } catch (FirebaseMessagingException e) {
            log.error("Échec d'envoi FCM ({})", description, e);
            throw new IllegalStateException("Échec d'envoi FCM: " + description, e);
        }
    }
}
