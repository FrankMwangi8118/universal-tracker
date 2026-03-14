package com.codify.universaltracker.validation.controller;

import com.codify.universaltracker.common.dto.ApiResponse;
import com.codify.universaltracker.validation.dto.CreateValidationRuleRequest;
import com.codify.universaltracker.validation.dto.UpdateValidationRuleRequest;
import com.codify.universaltracker.validation.dto.ValidationRuleResponse;
import com.codify.universaltracker.validation.service.ValidationRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ValidationRuleController {

    private final ValidationRuleService ruleService;

    public ValidationRuleController(ValidationRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping("/fields/{fieldId}/rules")
    public ResponseEntity<ApiResponse<ValidationRuleResponse>> add(
            @PathVariable UUID fieldId,
            @Valid @RequestBody CreateValidationRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ruleService.addRule(fieldId, request)));
    }

    @GetMapping("/fields/{fieldId}/rules")
    public ResponseEntity<ApiResponse<List<ValidationRuleResponse>>> list(@PathVariable UUID fieldId) {
        return ResponseEntity.ok(ApiResponse.ok(ruleService.listRules(fieldId)));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<ValidationRuleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateValidationRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(ruleService.updateRule(id, request)));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
