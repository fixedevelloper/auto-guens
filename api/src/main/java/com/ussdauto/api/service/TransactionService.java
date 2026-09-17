package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Device;
import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.domain.entity.TransactionStatusHistory;
import com.ussdauto.api.domain.enums.TransactionStatus;
import com.ussdauto.api.event.TransactionResultEvent;
import com.ussdauto.api.exception.InvalidStatusTransitionException;
import com.ussdauto.api.exception.TransactionNotFoundException;
import com.ussdauto.api.repository.TransactionRepository;
import com.ussdauto.api.repository.TransactionSpecifications;
import com.ussdauto.api.repository.TransactionStatusHistoryRepository;
import com.ussdauto.api.web.dto.CreateTransactionRequest;
import com.ussdauto.api.web.dto.TransactionFilterParams;
import com.ussdauto.api.web.dto.TransactionStatusCallbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStatusHistoryRepository statusHistoryRepository;
    private final DeviceSelectionService deviceSelectionService;
    private final UssdTemplateResolver ussdTemplateResolver;
    private final FcmNotificationService fcmNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${ussd-automation.transaction.timeout-seconds:90}")
    private long timeoutSeconds;

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest request) {
        String ussdCode = ussdTemplateResolver.resolve(
                request.operator(), request.operationType(), request.countryCode(), request.amount(), request.phone());

        DeviceAssignment assignment = deviceSelectionService.selectAndReserveDevice(request.operator());
        Device device = assignment.device();

        Transaction transaction = Transaction.builder()
                .operationType(request.operationType())
                .phone(request.phone())
                .operator(request.operator())
                .countryCode(request.countryCode())
                .amount(request.amount())
                .senderName(request.senderName())
                .description(request.description())
                .statut(TransactionStatus.PENDING)
                .deviceId(device.getId())
                .simSlotUsed(assignment.simSlotIndex())
                .ussdCodeUsed(ussdCode)
                .expiresAt(Instant.now().plus(timeoutSeconds, ChronoUnit.SECONDS))
                .build();

        transaction = transactionRepository.save(transaction);
        appendHistory(transaction.getId(), TransactionStatus.PENDING, null, null, null, null);

        fcmNotificationService.sendCommand(device, transaction, ussdCode, assignment.simSlotIndex());

        transaction.setStatut(TransactionStatus.SENT_TO_DEVICE);
        transaction = transactionRepository.save(transaction);
        appendHistory(transaction.getId(), TransactionStatus.SENT_TO_DEVICE, null, null, null, null);

        return transaction;
    }

    @Transactional(readOnly = true)
    public Transaction getById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<TransactionStatusHistory> getHistory(UUID transactionId) {
        return statusHistoryRepository.findByTransactionIdOrderByReceivedAtAsc(transactionId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> list(TransactionFilterParams filters) {
        return transactionRepository.findAll(TransactionSpecifications.fromFilters(filters));
    }

    /**
     * Applique un changement de statut rapporté par le device. Idempotent : si le
     * nouveau statut est identique au dernier statut connu, l'appel est journalisé
     * puis ignoré (aucune nouvelle ligne d'historique, aucun effet de bord).
     */
    @Transactional
    public Transaction applyStatusCallback(UUID transactionId, TransactionStatusCallbackRequest callback, String rawPayload) {
        if (!callback.status().isAllowedInCallback()) {
            throw new InvalidStatusTransitionException(callback.status());
        }

        Transaction transaction = getById(transactionId);

        TransactionStatus lastKnownStatus = statusHistoryRepository
                .findTopByTransactionIdOrderByReceivedAtDesc(transactionId)
                .map(TransactionStatusHistory::getStatut)
                .orElse(transaction.getStatut());

        if (lastKnownStatus == callback.status()) {
            log.info("Callback ignoré (idempotence) pour transaction {}: statut {} déjà enregistré",
                    transactionId, callback.status());
            return transaction;
        }

        appendHistory(transactionId, callback.status(), callback.rawSmsContent(), callback.operatorReference(),
                callback.failureReason(), rawPayload);

        transaction.setStatut(callback.status());
        if (callback.rawSmsContent() != null) {
            transaction.setSmsRawContent(callback.rawSmsContent());
        }
        if (callback.operatorReference() != null) {
            transaction.setOperatorReference(callback.operatorReference());
        }
        if (callback.failureReason() != null) {
            transaction.setFailureReason(callback.failureReason());
        }
        if (callback.status() == TransactionStatus.EXECUTING) {
            transaction.setExpiresAt(Instant.now().plus(timeoutSeconds, ChronoUnit.SECONDS));
        }

        transaction = transactionRepository.save(transaction);

        if (callback.status().isFinal()) {
            if (transaction.getDeviceId() != null) {
                deviceSelectionService.release(transaction.getDeviceId());
            }
            eventPublisher.publishEvent(new TransactionResultEvent(transaction));
        }

        return transaction;
    }

    private void appendHistory(UUID transactionId, TransactionStatus status, String rawSmsContent,
                                String operatorReference, String failureReason, String rawPayload) {
        statusHistoryRepository.save(TransactionStatusHistory.builder()
                .transactionId(transactionId)
                .statut(status)
                .receivedAt(Instant.now())
                .rawSmsContent(rawSmsContent)
                .operatorReference(operatorReference)
                .failureReason(failureReason)
                .rawPayload(rawPayload)
                .build());
    }
}
