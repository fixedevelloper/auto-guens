package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.UssdTemplate;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.exception.UssdTemplateNotFoundException;
import com.ussdauto.api.repository.UssdTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UssdTemplateResolverTest {

    @Mock
    private UssdTemplateRepository ussdTemplateRepository;

    @InjectMocks
    private UssdTemplateResolver ussdTemplateResolver;

    @Test
    void resolvesDepositTemplateForOperator() {
        UssdTemplate template = UssdTemplate.builder()
                .operator(Operator.MTN)
                .operationType(OperationType.DEPOSIT)
                .countryCode("CM")
                .template("*126*1*{amount}*{phone}#")
                .actif(true)
                .build();

        when(ussdTemplateRepository.findByOperatorAndOperationTypeAndCountryCodeAndActifTrue(Operator.MTN, OperationType.DEPOSIT, "CM"))
                .thenReturn(Optional.of(template));

        String ussdCode = ussdTemplateResolver.resolve(Operator.MTN, OperationType.DEPOSIT, "CM", new BigDecimal("5000"), "677000000");

        assertThat(ussdCode).isEqualTo("*126*1*5000*677000000#");
    }

    @Test
    void resolvesDifferentTemplateForWithdrawOnSameOperator() {
        UssdTemplate template = UssdTemplate.builder()
                .operator(Operator.MTN)
                .operationType(OperationType.WITHDRAW)
                .countryCode("CM")
                .template("*126*2*{amount}*{phone}#")
                .actif(true)
                .build();

        when(ussdTemplateRepository.findByOperatorAndOperationTypeAndCountryCodeAndActifTrue(Operator.MTN, OperationType.WITHDRAW, "CM"))
                .thenReturn(Optional.of(template));

        String ussdCode = ussdTemplateResolver.resolve(Operator.MTN, OperationType.WITHDRAW, "CM", new BigDecimal("2000"), "677000000");

        assertThat(ussdCode).isEqualTo("*126*2*2000*677000000#");
    }

    @Test
    void resolvesDifferentTemplateForSameOperatorInAnotherCountry() {
        UssdTemplate template = UssdTemplate.builder()
                .operator(Operator.MTN)
                .operationType(OperationType.DEPOSIT)
                .countryCode("CG")
                .template("*105*1*{amount}*{phone}#")
                .actif(true)
                .build();

        when(ussdTemplateRepository.findByOperatorAndOperationTypeAndCountryCodeAndActifTrue(Operator.MTN, OperationType.DEPOSIT, "CG"))
                .thenReturn(Optional.of(template));

        String ussdCode = ussdTemplateResolver.resolve(Operator.MTN, OperationType.DEPOSIT, "CG", new BigDecimal("5000"), "060000000");

        assertThat(ussdCode).isEqualTo("*105*1*5000*060000000#");
    }

    @Test
    void throwsWhenNoActiveTemplateForOperatorAndOperationTypeAndCountry() {
        when(ussdTemplateRepository.findByOperatorAndOperationTypeAndCountryCodeAndActifTrue(Operator.ORANGE, OperationType.DEPOSIT, "CM"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ussdTemplateResolver.resolve(Operator.ORANGE, OperationType.DEPOSIT, "CM", BigDecimal.TEN, "690000000"))
                .isInstanceOf(UssdTemplateNotFoundException.class);
    }
}
