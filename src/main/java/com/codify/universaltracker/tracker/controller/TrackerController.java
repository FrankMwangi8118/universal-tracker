package com.codify.universaltracker.tracker.controller;

import com.codify.universaltracker.common.dto.ApiResponse;
import com.codify.universaltracker.tracker.dto.CreateTrackerRequest;
import com.codify.universaltracker.tracker.dto.TrackerListResponse;
import com.codify.universaltracker.tracker.dto.TrackerResponse;
import com.codify.universaltracker.tracker.dto.UpdateTrackerRequest;
import com.codify.universaltracker.tracker.service.TrackerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TrackerController {

    private final TrackerService trackerService;

    public TrackerController(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @PostMapping("/trackers")
    public ResponseEntity<ApiResponse<TrackerResponse>> create(@Valid @RequestBody CreateTrackerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(trackerService.createTracker(request)));
    }

    @GetMapping("/domains/{domainSlug}/trackers")
    public ResponseEntity<ApiResponse<List<TrackerListResponse>>> listByDomain(@PathVariable String domainSlug) {
        return ResponseEntity.ok(ApiResponse.ok(trackerService.listTrackersByDomain(domainSlug)));
    }

    @GetMapping("/trackers")
    public ResponseEntity<ApiResponse<List<TrackerListResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(trackerService.listAllTrackers()));
    }

    @GetMapping("/trackers/{id}")
    public ResponseEntity<ApiResponse<TrackerResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(trackerService.getTracker(id)));
    }

    @PutMapping("/trackers/{id}")
    public ResponseEntity<ApiResponse<TrackerResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTrackerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(trackerService.updateTracker(id, request)));
    }

    @DeleteMapping("/trackers/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        trackerService.deleteTracker(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/trackers/{id}/reorder")
    public ResponseEntity<Void> reorder(@PathVariable UUID id, @RequestBody List<UUID> orderedIds) {
        trackerService.reorderTrackers(id, orderedIds);
        return ResponseEntity.ok().build();
    }
}
