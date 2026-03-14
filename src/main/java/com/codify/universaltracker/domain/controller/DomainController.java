package com.codify.universaltracker.domain.controller;

import com.codify.universaltracker.common.dto.ApiResponse;
import com.codify.universaltracker.domain.dto.CreateDomainRequest;
import com.codify.universaltracker.domain.dto.DomainListResponse;
import com.codify.universaltracker.domain.dto.DomainResponse;
import com.codify.universaltracker.domain.dto.UpdateDomainRequest;
import com.codify.universaltracker.domain.service.DomainService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DomainResponse>> create(@Valid @RequestBody CreateDomainRequest request) {
        DomainResponse response = domainService.createDomain(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DomainListResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(domainService.listDomains()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<DomainResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(domainService.getDomainBySlug(slug)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DomainResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDomainRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(domainService.updateDomain(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        domainService.deleteDomain(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody List<UUID> orderedIds) {
        domainService.reorderDomains(orderedIds);
        return ResponseEntity.ok().build();
    }
}
