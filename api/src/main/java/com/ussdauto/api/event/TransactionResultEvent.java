package com.ussdauto.api.event;

import com.ussdauto.api.domain.entity.Transaction;

/** Publié quand une transaction atteint un statut final (SUCCESS/FAILED/TIMEOUT). */
public record TransactionResultEvent(Transaction transaction) {
}
