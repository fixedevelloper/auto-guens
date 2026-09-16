package com.ussdauto.api.web.mapper;

import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.domain.entity.TransactionStatusHistory;
import com.ussdauto.api.web.dto.StatusHistoryEntryResponse;
import com.ussdauto.api.web.dto.TransactionResponse;
import com.ussdauto.api.web.dto.TransactionSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction, List<TransactionStatusHistory> history) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getOperationType(),
                transaction.getPhone(),
                transaction.getOperator(),
                transaction.getCountryCode(),
                transaction.getAmount(),
                transaction.getSenderName(),
                transaction.getDescription(),
                transaction.getStatut(),
                transaction.getDeviceId() != null ? transaction.getDeviceId().toString() : null,
                transaction.getSimSlotUsed(),
                transaction.getUssdCodeUsed(),
                transaction.getOperatorReference(),
                transaction.getFailureReason(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getExpiresAt(),
                history.stream().map(this::toHistoryEntry).toList()
        );
    }

    public TransactionSummaryResponse toSummary(Transaction transaction) {
        return new TransactionSummaryResponse(
                transaction.getId(),
                transaction.getOperationType(),
                transaction.getPhone(),
                transaction.getOperator(),
                transaction.getCountryCode(),
                transaction.getAmount(),
                transaction.getSenderName(),
                transaction.getStatut(),
                transaction.getOperatorReference(),
                transaction.getFailureReason(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private StatusHistoryEntryResponse toHistoryEntry(TransactionStatusHistory entry) {
        return new StatusHistoryEntryResponse(
                entry.getStatut(),
                entry.getReceivedAt(),
                entry.getRawSmsContent(),
                entry.getOperatorReference(),
                entry.getFailureReason()
        );
    }
}
