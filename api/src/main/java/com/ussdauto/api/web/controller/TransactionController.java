package com.ussdauto.api.web.controller;

import com.ussdauto.api.domain.entity.Transaction;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.domain.enums.TransactionStatus;
import com.ussdauto.api.service.TransactionService;
import com.ussdauto.api.web.dto.CreateTransactionRequest;
import com.ussdauto.api.web.dto.TransactionFilterParams;
import com.ussdauto.api.web.dto.TransactionResponse;
import com.ussdauto.api.web.dto.TransactionSummaryResponse;
import com.ussdauto.api.web.mapper.TransactionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request);
        TransactionResponse response = transactionMapper.toResponse(transaction, transactionService.getHistory(transaction.getId()));
        return ResponseEntity.created(URI.create("/api/transactions/" + transaction.getId())).body(response);
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@PathVariable UUID id) {
        Transaction transaction = transactionService.getById(id);
        return transactionMapper.toResponse(transaction, transactionService.getHistory(id));
    }

    @GetMapping
    public List<TransactionSummaryResponse> list(
            @RequestParam(required = false) OperationType operationType,
            @RequestParam(required = false) Operator operator,
            @RequestParam(required = false) TransactionStatus statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        TransactionFilterParams filters = new TransactionFilterParams(operationType, operator, statut, from, to);
        return transactionService.list(filters).stream().map(transactionMapper::toSummary).toList();
    }
}
