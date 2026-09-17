package com.ussdauto.api.web.controller;

import com.ussdauto.api.domain.entity.UssdTemplate;
import com.ussdauto.api.domain.enums.Operator;
import com.ussdauto.api.domain.enums.OperationType;
import com.ussdauto.api.service.UssdTemplateService;
import com.ussdauto.api.web.dto.CreateUssdTemplateRequest;
import com.ussdauto.api.web.dto.UpdateUssdTemplateRequest;
import com.ussdauto.api.web.dto.UssdTemplateResponse;
import com.ussdauto.api.web.mapper.UssdTemplateMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ussd-templates")
@RequiredArgsConstructor
public class UssdTemplateController {

    private final UssdTemplateService ussdTemplateService;
    private final UssdTemplateMapper ussdTemplateMapper;

    @PostMapping
    public ResponseEntity<UssdTemplateResponse> create(@Valid @RequestBody CreateUssdTemplateRequest request) {
        UssdTemplate template = ussdTemplateService.create(request);
        UssdTemplateResponse response = ussdTemplateMapper.toResponse(template);
        return ResponseEntity.created(URI.create("/api/ussd-templates/" + template.getId())).body(response);
    }

    @GetMapping
    public List<UssdTemplateResponse> list(
            @RequestParam Operator operator,
            @RequestParam(required = false) OperationType operationType,
            @RequestParam(required = false) String countryCode
    ) {
        return ussdTemplateService.list(operator, operationType, countryCode).stream().map(ussdTemplateMapper::toResponse).toList();
    }

    @PutMapping("/{id}")
    public UssdTemplateResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUssdTemplateRequest request) {
        return ussdTemplateMapper.toResponse(ussdTemplateService.update(id, request));
    }
}
