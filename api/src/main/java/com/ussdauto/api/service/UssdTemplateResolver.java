package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.UssdTemplate;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.exception.UssdTemplateNotFoundException;
import com.ussdauto.api.repository.UssdTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Résout le code USSD à composer pour un (operator, operationType) donné — chaque
 * opérateur ayant un menu USSD marchand distinct pour DEPOSIT et pour WITHDRAW.
 */
@Service
@RequiredArgsConstructor
public class UssdTemplateResolver {

    private final UssdTemplateRepository ussdTemplateRepository;

    public String resolve(Operator operator, OperationType operationType, BigDecimal amount, String phone) {
        UssdTemplate template = ussdTemplateRepository
                .findByOperatorAndOperationTypeAndActifTrue(operator, operationType)
                .orElseThrow(() -> new UssdTemplateNotFoundException(operator, operationType));

        return template.resolve(amount.toPlainString(), phone);
    }
}
