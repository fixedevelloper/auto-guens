package com.ussdauto.api.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.exception.InvalidTransactionStateException;
import com.ussdauto.api.repository.DeviceRepository;
import com.ussdauto.api.service.TransactionService;
import com.ussdauto.api.web.dto.TransactionResponse;
import com.ussdauto.api.web.dto.TransactionStatusCallbackRequest;
import com.ussdauto.api.web.mapper.TransactionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionStatusCallbackController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/{id}/status")
    public TransactionResponse reportStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionStatusCallbackRequest callback,
            Authentication authentication
    ) {
        Transaction transaction = transactionService.getById(id);
        assertCallbackFromAssignedDevice(transaction, authentication);

        // Le body brut exact n'est plus disponible ici (déjà désérialisé) ; on ré-encode le DTO
        // validé comme approximation d'audit fonctionnellement équivalente au JSON reçu.
        String rawPayload = writeAsJson(callback);

        Transaction updated = transactionService.applyStatusCallback(id, callback, rawPayload);
        return transactionMapper.toResponse(updated, transactionService.getHistory(id));
    }

    private void assertCallbackFromAssignedDevice(Transaction transaction, Authentication authentication) {
        String callingDeviceApiKey = (String) authentication.getPrincipal();

        if (transaction.getDeviceId() == null) {
            return;
        }

        deviceRepository.findById(transaction.getDeviceId()).ifPresent(device -> {
            if (!device.getApiKey().equals(callingDeviceApiKey)) {
                throw new InvalidTransactionStateException(
                        "Ce device n'est pas celui assigné à la transaction " + transaction.getId());
            }
        });
    }

    private String writeAsJson(TransactionStatusCallbackRequest callback) {
        try {
            return objectMapper.writeValueAsString(callback);
        } catch (Exception e) {
            return null;
        }
    }
}
