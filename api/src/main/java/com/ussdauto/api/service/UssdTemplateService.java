package com.ussdauto.api.service;

import com.ussdauto.api.domain.entity.UssdTemplate;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.exception.UssdTemplateNotFoundException;
import com.ussdauto.api.repository.UssdTemplateRepository;
import com.ussdauto.api.web.dto.CreateUssdTemplateRequest;
import com.ussdauto.api.web.dto.UpdateUssdTemplateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UssdTemplateService {

    private final UssdTemplateRepository ussdTemplateRepository;

    /** Désactive l'ancien template actif du couple (operator, operationType), s'il existe. */
    @Transactional
    public UssdTemplate create(CreateUssdTemplateRequest request) {
        ussdTemplateRepository.findByOperatorAndOperationTypeAndActifTrue(request.operator(), request.operationType())
                .ifPresent(previous -> {
                    previous.setActif(false);
                    ussdTemplateRepository.save(previous);
                });

        UssdTemplate template = UssdTemplate.builder()
                .operator(request.operator())
                .operationType(request.operationType())
                .template(request.template())
                .actif(true)
                .build();

        return ussdTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<UssdTemplate> list(Operator operator, OperationType operationType) {
        if (operationType != null) {
            return ussdTemplateRepository.findByOperatorAndOperationType(operator, operationType);
        }
        return ussdTemplateRepository.findByOperator(operator);
    }

    @Transactional
    public UssdTemplate update(UUID id, UpdateUssdTemplateRequest request) {
        UssdTemplate template = ussdTemplateRepository.findById(id)
                .orElseThrow(() -> new UssdTemplateNotFoundException(id));

        template.setTemplate(request.template());
        if (request.actif() != null) {
            template.setActif(request.actif());
        }

        return ussdTemplateRepository.save(template);
    }
}
