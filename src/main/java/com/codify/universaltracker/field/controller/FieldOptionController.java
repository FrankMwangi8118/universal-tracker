package com.codify.universaltracker.field.controller;

import com.codify.universaltracker.common.dto.ApiResponse;
import com.codify.universaltracker.field.dto.CreateFieldOptionRequest;
import com.codify.universaltracker.field.dto.FieldOptionResponse;
import com.codify.universaltracker.field.dto.UpdateFieldOptionRequest;
import com.codify.universaltracker.field.service.FieldDefinitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fields/{fieldId}/options")
public class FieldOptionController {

    private final FieldDefinitionService fieldService;

    public FieldOptionController(FieldDefinitionService fieldService) {
        this.fieldService = fieldService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FieldOptionResponse>> add(
            @PathVariable UUID fieldId,
            @Valid @RequestBody CreateFieldOptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(fieldService.addOption(fieldId, request)));
    }

    @PutMapping("/{optionId}")
    public ResponseEntity<ApiResponse<FieldOptionResponse>> update(
            @PathVariable UUID fieldId,
            @PathVariable UUID optionId,
            @Valid @RequestBody UpdateFieldOptionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(fieldService.updateOption(fieldId, optionId, request)));
    }

    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID fieldId,
            @PathVariable UUID optionId) {
        fieldService.deleteOption(fieldId, optionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID fieldId,
            @RequestBody List<UUID> orderedIds) {
        fieldService.reorderOptions(fieldId, orderedIds);
        return ResponseEntity.ok().build();
    }
}
