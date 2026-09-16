package com.ussdauto.api.exception;

import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;

public class UssdTemplateNotFoundException extends RuntimeException {

    public UssdTemplateNotFoundException(Operator operator, OperationType operationType) {
        super("Aucun template USSD actif pour operator=" + operator + " et operationType=" + operationType);
    }

    public UssdTemplateNotFoundException(java.util.UUID id) {
        super("Template USSD introuvable : " + id);
    }
}
