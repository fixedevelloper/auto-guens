package com.ussdauto.api.validation;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.web.dto.CreateTransactionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTransactionRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    @Test
    void rejectsAMissingOperationType() {
        var request = new CreateTransactionRequest(
                null, "677000000", Operator.MTN, new BigDecimal("5000"), "Jean Dupont", "CM", null);

        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("operationType");
    }

    @Test
    void rejectsANonPositiveAmount() {
        var request = new CreateTransactionRequest(
                OperationType.DEPOSIT, "677000000", Operator.MTN, BigDecimal.ZERO, "Jean Dupont", "CM", null);

        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void acceptsAWellFormedDepositRequest() {
        var request = new CreateTransactionRequest(
                OperationType.DEPOSIT, "677000000", Operator.MTN, new BigDecimal("5000"), "Jean Dupont", "CM", "desc");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsAWellFormedWithdrawRequestWithoutDescription() {
        var request = new CreateTransactionRequest(
                OperationType.WITHDRAW, "690000000", Operator.ORANGE, new BigDecimal("2000"), "Jean Dupont", "CM", null);

        assertThat(validator.validate(request)).isEmpty();
    }
}
