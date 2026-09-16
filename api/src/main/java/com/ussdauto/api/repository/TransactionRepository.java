package com.ussdauto.api.repository;

import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByStatutInAndExpiresAtBefore(Collection<TransactionStatus> statuts, Instant instant);
}
