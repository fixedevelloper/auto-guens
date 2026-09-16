package com.ussdauto.api.repository;

import com.ussdauto.api.domain.entity.TransactionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionStatusHistoryRepository extends JpaRepository<TransactionStatusHistory, UUID> {

    List<TransactionStatusHistory> findByTransactionIdOrderByReceivedAtAsc(UUID transactionId);

    Optional<TransactionStatusHistory> findTopByTransactionIdOrderByReceivedAtDesc(UUID transactionId);
}
