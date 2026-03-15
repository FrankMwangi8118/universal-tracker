package com.codify.universaltracker.view.controller;

import com.codify.universaltracker.common.dto.ApiResponse;
import com.codify.universaltracker.view.dto.*;
import com.codify.universaltracker.view.service.AggregationService;
import com.codify.universaltracker.view.service.DashboardService;
import com.codify.universaltracker.view.service.ViewConfigService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ViewController {

    private final ViewConfigService viewService;
    private final AggregationService aggregationService;
    private final DashboardService dashboardService;

    public ViewController(ViewConfigService viewService,
                          AggregationService aggregationService,
                          DashboardService dashboardService) {
        this.viewService = viewService;
        this.aggregationService = aggregationService;
        this.dashboardService = dashboardService;
    }



    @PostMapping("/trackers/{trackerId}/views")
    public ResponseEntity<ApiResponse<ViewConfigResponse>> create(
            @PathVariable UUID trackerId,
            @Valid @RequestBody CreateViewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(viewService.createView(trackerId, request)));
    }

    @GetMapping("/trackers/{trackerId}/views")
    public ResponseEntity<ApiResponse<List<ViewConfigResponse>>> list(@PathVariable UUID trackerId) {
        return ResponseEntity.ok(ApiResponse.ok(viewService.listViews(trackerId)));
    }

    @GetMapping("/views/{id}")
    public ResponseEntity<ApiResponse<ViewConfigResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(viewService.getView(id)));
    }

    @PutMapping("/views/{id}")
    public ResponseEntity<ApiResponse<ViewConfigResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateViewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(viewService.updateView(id, request)));
    }

    @DeleteMapping("/views/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        viewService.deleteView(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/views/{id}/default")
    public ResponseEntity<ApiResponse<ViewConfigResponse>> setDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(viewService.setDefaultView(id)));
    }

    @PostMapping("/views/{id}/duplicate")
    public ResponseEntity<ApiResponse<ViewConfigResponse>> duplicate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(viewService.duplicateView(id)));
    }

    // -------------------------------------------------------------------------
    // Aggregation

    @GetMapping("/trackers/{trackerId}/aggregate")
    public ResponseEntity<ApiResponse<AggregateResponse>> aggregate(
            @PathVariable UUID trackerId,
            @RequestParam String field,
            @RequestParam(defaultValue = "SUM") String fn,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String dateBucket,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        AggregateResponse result = aggregationService.aggregate(
                trackerId, field, fn, groupBy, dateBucket, from, to);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // -------------------------------------------------------------------------
    // Dashboard

    @GetMapping("/trackers/{trackerId}/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard(@PathVariable UUID trackerId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(trackerId)));
    }
}
