package com.codify.universaltracker.automation.controller;

import com.codify.universaltracker.automation.dto.*;
import com.codify.universaltracker.automation.service.AutomationService;
import com.codify.universaltracker.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AutomationController {

    private final AutomationService automationService;

    public AutomationController(AutomationService automationService) {
        this.automationService = automationService;
    }

    @PostMapping("/trackers/{trackerId}/automations")
    public ResponseEntity<ApiResponse<AutomationResponse>> create(
            @PathVariable UUID trackerId,
            @Valid @RequestBody CreateAutomationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(automationService.create(trackerId, request)));
    }

    @GetMapping("/trackers/{trackerId}/automations")
    public ResponseEntity<ApiResponse<List<AutomationResponse>>> list(@PathVariable UUID trackerId) {
        return ResponseEntity.ok(ApiResponse.ok(automationService.list(trackerId)));
    }

    @GetMapping("/automations/{id}")
    public ResponseEntity<ApiResponse<AutomationResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(automationService.get(id)));
    }

    @PutMapping("/automations/{id}")
    public ResponseEntity<ApiResponse<AutomationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAutomationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(automationService.update(id, request)));
    }

    @DeleteMapping("/automations/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        automationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/automations/{id}/toggle")
    public ResponseEntity<ApiResponse<AutomationResponse>> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(automationService.toggle(id)));
    }

    @GetMapping("/automations/{id}/logs")
    public ResponseEntity<ApiResponse<List<AutomationLogResponse>>> logs(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AutomationLogResponse> result = automationService.getLogs(id, page, size);
        return ResponseEntity.ok(ApiResponse.paginated(result.getContent(), result.getTotalElements(), page, size));
    }
}
