package com.ussdauto.api.repository;

import com.ussdauto.api.domain.entity.UssdTemplate;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UssdTemplateRepository extends JpaRepository<UssdTemplate, UUID> {

    Optional<UssdTemplate> findByOperatorAndOperationTypeAndCountryCodeAndActifTrue(
            Operator operator, OperationType operationType, String countryCode);

    List<UssdTemplate> findByOperator(Operator operator);

    List<UssdTemplate> findByOperatorAndCountryCode(Operator operator, String countryCode);

    List<UssdTemplate> findByOperatorAndOperationType(Operator operator, OperationType operationType);

    List<UssdTemplate> findByOperatorAndOperationTypeAndCountryCode(
            Operator operator, OperationType operationType, String countryCode);
}
