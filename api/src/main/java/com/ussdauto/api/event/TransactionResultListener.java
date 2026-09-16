package com.ussdauto.api.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransactionResultListener {

    @Async
    @EventListener
    public void onTransactionResult(TransactionResultEvent event) {
        // Point d'extension : notifier le marchand (webhook HTTP, message queue, etc.)
        log.info("Transaction {} terminée avec le statut {}",
                event.transaction().getId(), event.transaction().getStatut());
    }
}
