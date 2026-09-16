package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.domain.entity.TransactionStatusHistory;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.domain.enums.TransactionStatus;
import com.ussdauto.api.event.TransactionResultEvent;
import com.ussdauto.api.exception.InvalidStatusTransitionException;
import com.ussdauto.api.repository.TransactionRepository;
import com.ussdauto.api.repository.TransactionStatusHistoryRepository;
import com.ussdauto.api.web.dto.TransactionStatusCallbackRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceStatusCallbackTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionStatusHistoryRepository statusHistoryRepository;
    @Mock
    private DeviceSelectionService deviceSelectionService;
    @Mock
    private UssdTemplateResolver ussdTemplateResolver;
    @Mock
    private FcmNotificationService fcmNotificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    private UUID transactionId;
    private UUID deviceId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        deviceId = UUID.randomUUID();
    }

    @Test
    void appendsAHistoryEntryAndUpdatesCurrentStatusForANonFinalTransition() {
        Transaction transaction = transaction(TransactionStatus.SENT_TO_DEVICE);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(statusHistoryRepository.findTopByTransactionIdOrderByReceivedAtDesc(transactionId)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var callback = new TransactionStatusCallbackRequest(
                TransactionStatus.RECEIVED_BY_DEVICE, Instant.now(), null, null, null);

        Transaction result = transactionService.applyStatusCallback(transactionId, callback, "{}");

        assertThat(result.getStatut()).isEqualTo(TransactionStatus.RECEIVED_BY_DEVICE);
        ArgumentCaptor<TransactionStatusHistory> historyCaptor = ArgumentCaptor.forClass(TransactionStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatut()).isEqualTo(TransactionStatus.RECEIVED_BY_DEVICE);
        verify(deviceSelectionService, never()).release(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void releasesDeviceAndPublishesEventOnFinalStatus() {
        Transaction transaction = transaction(TransactionStatus.EXECUTING);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(statusHistoryRepository.findTopByTransactionIdOrderByReceivedAtDesc(transactionId))
                .thenReturn(Optional.of(historyEntry(TransactionStatus.EXECUTING)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var callback = new TransactionStatusCallbackRequest(
                TransactionStatus.SUCCESS, Instant.now(), "SMS content", "OP-REF-1", null);

        Transaction result = transactionService.applyStatusCallback(transactionId, callback, "{}");

        assertThat(result.getStatut()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(result.getOperatorReference()).isEqualTo("OP-REF-1");
        verify(deviceSelectionService).release(deviceId);
        verify(eventPublisher).publishEvent(any(TransactionResultEvent.class));
    }

    @Test
    void ignoresCallbackWhenSameStatusReportedTwiceInARow() {
        Transaction transaction = transaction(TransactionStatus.EXECUTING);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(statusHistoryRepository.findTopByTransactionIdOrderByReceivedAtDesc(transactionId))
                .thenReturn(Optional.of(historyEntry(TransactionStatus.EXECUTING)));

        var callback = new TransactionStatusCallbackRequest(
                TransactionStatus.EXECUTING, Instant.now(), "late duplicate", null, null);

        Transaction result = transactionService.applyStatusCallback(transactionId, callback, "{}");

        assertThat(result.getStatut()).isEqualTo(TransactionStatus.EXECUTING);
        verify(statusHistoryRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(deviceSelectionService, never()).release(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsAStatusNotAllowedInCallback() {
        var callback = new TransactionStatusCallbackRequest(
                TransactionStatus.TIMEOUT, Instant.now(), null, null, null);

        assertThatThrownBy(() -> transactionService.applyStatusCallback(transactionId, callback, "{}"))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verifyNoInteractions(transactionRepository, statusHistoryRepository);
    }

    private Transaction transaction(TransactionStatus statut) {
        return Transaction.builder()
                .id(transactionId)
                .operationType(OperationType.DEPOSIT)
                .phone("677000000")
                .operator(Operator.MTN)
                .countryCode("CM")
                .amount(new BigDecimal("5000"))
                .senderName("Jean Dupont")
                .statut(statut)
                .deviceId(deviceId)
                .build();
    }

    private TransactionStatusHistory historyEntry(TransactionStatus statut) {
        return TransactionStatusHistory.builder()
                .transactionId(transactionId)
                .statut(statut)
                .receivedAt(Instant.now())
                .build();
    }
}
