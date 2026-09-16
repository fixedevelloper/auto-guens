package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.domain.entity.TransactionStatusHistory;
import com.ussdauto.api.domain.enums.TransactionStatus;
import com.ussdauto.api.event.TransactionResultEvent;
import com.ussdauto.api.repository.TransactionRepository;
import com.ussdauto.api.repository.TransactionStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Bascule en TIMEOUT les transactions bloquées en SENT_TO_DEVICE ou EXECUTING depuis
 * plus de [timeout-seconds], journalise la transition, et libère le device associé.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionTimeoutJob {

    private final TransactionRepository transactionRepository;
    private final TransactionStatusHistoryRepository statusHistoryRepository;
    private final DeviceSelectionService deviceSelectionService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${ussd-automation.transaction.timeout-job-fixed-delay-ms:30000}")
    @Transactional
    public void timeoutStaleTransactions() {
        List<Transaction> staleTransactions = transactionRepository
                .findByStatutInAndExpiresAtBefore(TransactionStatus.timeoutEligible(), Instant.now());

        for (Transaction transaction : staleTransactions) {
            transaction.setStatut(TransactionStatus.TIMEOUT);
            transaction.setFailureReason("TIMEOUT: aucune progression du device dans le délai imparti");
            transactionRepository.save(transaction);

            statusHistoryRepository.save(TransactionStatusHistory.builder()
                    .transactionId(transaction.getId())
                    .statut(TransactionStatus.TIMEOUT)
                    .receivedAt(Instant.now())
                    .failureReason(transaction.getFailureReason())
                    .build());

            if (transaction.getDeviceId() != null) {
                deviceSelectionService.release(transaction.getDeviceId());
            }

            eventPublisher.publishEvent(new TransactionResultEvent(transaction));
            log.warn("Transaction {} basculée en TIMEOUT", transaction.getId());
        }
    }
}
