package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.domain.enums.TransactionStatus;
import com.ussdauto.api.event.TransactionResultEvent;
import com.ussdauto.api.repository.TransactionRepository;
import com.ussdauto.api.repository.TransactionStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionTimeoutJobTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionStatusHistoryRepository statusHistoryRepository;
    @Mock
    private DeviceSelectionService deviceSelectionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransactionTimeoutJob transactionTimeoutJob;

    @Test
    void timesOutStaleTransactionsAndReleasesTheirDevice() {
        UUID deviceId = UUID.randomUUID();
        Transaction staleTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .operationType(OperationType.WITHDRAW)
                .phone("677000000")
                .operator(Operator.ORANGE)
                .countryCode("CM")
                .amount(new BigDecimal("1000"))
                .senderName("Jean Dupont")
                .statut(TransactionStatus.EXECUTING)
                .deviceId(deviceId)
                .expiresAt(Instant.now().minusSeconds(5))
                .build();

        when(transactionRepository.findByStatutInAndExpiresAtBefore(eq(TransactionStatus.timeoutEligible()), any(Instant.class)))
                .thenReturn(List.of(staleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionTimeoutJob.timeoutStaleTransactions();

        assertThat(staleTransaction.getStatut()).isEqualTo(TransactionStatus.TIMEOUT);
        assertThat(staleTransaction.getFailureReason()).isNotBlank();
        verify(statusHistoryRepository).save(any());
        verify(deviceSelectionService).release(deviceId);
        verify(eventPublisher).publishEvent(any(TransactionResultEvent.class));
    }

    @Test
    void doesNothingWhenNoTransactionIsStale() {
        when(transactionRepository.findByStatutInAndExpiresAtBefore(eq(TransactionStatus.timeoutEligible()), any(Instant.class)))
                .thenReturn(List.of());

        transactionTimeoutJob.timeoutStaleTransactions();

        verify(transactionRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
        verify(deviceSelectionService, never()).release(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
