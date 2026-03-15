package com.codify.universaltracker.entry.controller;

import com.codify.universaltracker.common.dto.ApiResponse;
import com.codify.universaltracker.entry.dto.*;
import com.codify.universaltracker.entry.service.EntryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @PostMapping("/trackers/{trackerId}/entries")
    public ResponseEntity<ApiResponse<EntryResponse>> create(
            @PathVariable UUID trackerId,
            @RequestBody CreateEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(entryService.createEntry(trackerId, request)));
    }

    @GetMapping("/trackers/{trackerId}/entries")
    public ResponseEntity<ApiResponse<List<EntryListResponse>>> list(
            @PathVariable UUID trackerId,
            @ModelAttribute EntryFilterRequest filter,
            @RequestParam(required = false) Map<String, String> allParams) {

        filter.parseFieldFilters(allParams != null ? allParams : Map.of());
        Page<EntryListResponse> page = entryService.listEntries(trackerId, filter);

        return ResponseEntity.ok(ApiResponse.paginated(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        ));
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<ApiResponse<EntryResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(entryService.getEntry(id)));
    }

    @PutMapping("/entries/{id}")
    public ResponseEntity<ApiResponse<EntryResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(entryService.updateEntry(id, request)));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        entryService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/entries/{id}/restore")
    public ResponseEntity<ApiResponse<EntryResponse>> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(entryService.restoreEntry(id)));
    }

    @DeleteMapping("/entries/{id}/permanent")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID id) {
        entryService.hardDeleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trackers/{trackerId}/entries/bulk")
    public ResponseEntity<ApiResponse<List<EntryResponse>>> bulkCreate(
            @PathVariable UUID trackerId,
            @RequestBody List<CreateEntryRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(entryService.bulkCreateEntries(trackerId, requests)));
    }

    @DeleteMapping("/entries/bulk")
    public ResponseEntity<Void> bulkDelete(@Valid @RequestBody BulkDeleteRequest request) {
        entryService.bulkDeleteEntries(request.ids());
        return ResponseEntity.noContent().build();
    }
}
