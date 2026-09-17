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
 * Résout le code USSD à composer pour un (operator, operationType, countryCode) donné —
 * chaque opérateur ayant un menu USSD marchand distinct pour DEPOSIT et pour WITHDRAW,
 * et différent d'un pays à l'autre pour un même operator (ex. MTN Cameroun vs MTN Congo).
 */
@Service
@RequiredArgsConstructor
public class UssdTemplateResolver {

    private final UssdTemplateRepository ussdTemplateRepository;

    public String resolve(Operator operator, OperationType operationType, String countryCode, BigDecimal amount, String phone) {
        UssdTemplate template = ussdTemplateRepository
                .findByOperatorAndOperationTypeAndCountryCodeAndActifTrue(operator, operationType, countryCode)
                .orElseThrow(() -> new UssdTemplateNotFoundException(operator, operationType, countryCode));

        return template.resolve(amount.toPlainString(), phone);
    }
}
