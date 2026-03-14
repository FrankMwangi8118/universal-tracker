package com.codify.universaltracker.field.controller;

import com.codify.universaltracker.common.dto.ApiResponse;
import com.codify.universaltracker.field.dto.*;
import com.codify.universaltracker.field.service.FieldDefinitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FieldDefinitionController {

    private final FieldDefinitionService fieldService;

    public FieldDefinitionController(FieldDefinitionService fieldService) {
        this.fieldService = fieldService;
    }

    @PostMapping("/trackers/{trackerId}/fields")
    public ResponseEntity<ApiResponse<FieldDefinitionResponse>> create(
            @PathVariable UUID trackerId,
            @Valid @RequestBody CreateFieldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(fieldService.createField(trackerId, request)));
    }

    @GetMapping("/trackers/{trackerId}/fields")
    public ResponseEntity<ApiResponse<List<FieldDefinitionListResponse>>> list(@PathVariable UUID trackerId) {
        return ResponseEntity.ok(ApiResponse.ok(fieldService.listFields(trackerId)));
    }

    @GetMapping("/fields/{id}")
    public ResponseEntity<ApiResponse<FieldDefinitionResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(fieldService.getField(id)));
    }

    @PutMapping("/fields/{id}")
    public ResponseEntity<ApiResponse<FieldDefinitionResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFieldRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(fieldService.updateField(id, request)));
    }

    @DeleteMapping("/fields/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fieldService.deleteField(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/trackers/{trackerId}/fields/reorder")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID trackerId,
            @RequestBody List<UUID> orderedIds) {
        fieldService.reorderFields(trackerId, orderedIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fields/{id}/duplicate")
    public ResponseEntity<ApiResponse<FieldDefinitionResponse>> duplicate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(fieldService.duplicateField(id)));
    }
}
