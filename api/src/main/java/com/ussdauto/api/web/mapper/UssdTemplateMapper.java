package com.ussdauto.api.web.mapper;

import com.ussdauto.api.domain.entity.UssdTemplate;
import com.ussdauto.api.web.dto.UssdTemplateResponse;
import org.springframework.stereotype.Component;

@Component
public class UssdTemplateMapper {

    public UssdTemplateResponse toResponse(UssdTemplate template) {
        return new UssdTemplateResponse(
                template.getId(),
                template.getOperator(),
                template.getOperationType(),
                template.getTemplate(),
                template.isActif(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
