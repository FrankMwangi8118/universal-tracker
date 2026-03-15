package com.codify.universaltracker.view.service;

import com.codify.universaltracker.common.exception.BusinessRuleException;
import com.codify.universaltracker.common.exception.ResourceNotFoundException;
import com.codify.universaltracker.tracker.service.TrackerService;
import com.codify.universaltracker.view.dto.CreateViewRequest;
import com.codify.universaltracker.view.dto.UpdateViewRequest;
import com.codify.universaltracker.view.dto.ViewConfigResponse;
import com.codify.universaltracker.view.entity.ViewConfig;
import com.codify.universaltracker.view.repository.ViewConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ViewConfigService {

    private static final Logger log = LoggerFactory.getLogger(ViewConfigService.class);

    private final ViewConfigRepository viewRepo;
    private final TrackerService trackerService;

    public ViewConfigService(ViewConfigRepository viewRepo, TrackerService trackerService) {
        this.viewRepo = viewRepo;
        this.trackerService = trackerService;
    }

    @Transactional
    public ViewConfigResponse createView(UUID trackerId, CreateViewRequest request) {
        trackerService.findOwnedTracker(trackerId);

        ViewConfig view = new ViewConfig();
        view.setTrackerId(trackerId);
        applyRequest(view, request);

        // If this is the first view or explicitly set as default, clear others
        if (Boolean.TRUE.equals(request.isDefault()) || viewRepo.countByTrackerId(trackerId) == 0) {
            view.setIsDefault(true);
            viewRepo.clearDefaultsExcept(trackerId, UUID.randomUUID()); // clear all, then set this one
        }

        ViewConfig saved = viewRepo.save(view);

        // Ensure default flag is re-cleared for others after save
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            viewRepo.clearDefaultsExcept(trackerId, saved.getId());
        }

        log.info("Created view '{}' (type={}) for tracker {}", saved.getName(), saved.getViewType(), trackerId);
        return ViewConfigResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ViewConfigResponse> listViews(UUID trackerId) {
        trackerService.findOwnedTracker(trackerId);
        return viewRepo.findByTrackerIdOrderBySortOrderAsc(trackerId)
                .stream().map(ViewConfigResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ViewConfigResponse getView(UUID viewId) {
        return ViewConfigResponse.from(findView(viewId));
    }

    @Transactional
    public ViewConfigResponse updateView(UUID viewId, UpdateViewRequest request) {
        ViewConfig view = findView(viewId);
        applyUpdate(view, request);
        ViewConfig saved = viewRepo.save(view);
        log.info("Updated view id={}", viewId);
        return ViewConfigResponse.from(saved);
    }

    @Transactional
    public void deleteView(UUID viewId) {
        ViewConfig view = findView(viewId);
        if (Boolean.TRUE.equals(view.getIsDefault())) {
            int total = viewRepo.countByTrackerId(view.getTrackerId());
            if (total <= 1) {
                throw new BusinessRuleException("Cannot delete the last remaining view for this tracker");
            }
        }
        viewRepo.delete(view);
        log.info("Deleted view id={}", viewId);
    }

    @Transactional
    public ViewConfigResponse setDefaultView(UUID viewId) {
        ViewConfig view = findView(viewId);
        viewRepo.clearDefaultsExcept(view.getTrackerId(), viewId);
        view.setIsDefault(true);
        return ViewConfigResponse.from(viewRepo.save(view));
    }

    @Transactional
    public ViewConfigResponse duplicateView(UUID viewId) {
        ViewConfig original = findView(viewId);

        ViewConfig copy = new ViewConfig();
        copy.setTrackerId(original.getTrackerId());
        copy.setName(original.getName() + " (copy)");
        copy.setViewType(original.getViewType());
        copy.setColumns(original.getColumns());
        copy.setSortRules(original.getSortRules());
        copy.setFilterRules(original.getFilterRules());
        copy.setGroupBy(original.getGroupBy());
        copy.setAggregations(original.getAggregations());
        copy.setChartConfig(original.getChartConfig());
        copy.setIsDefault(false);
        copy.setSortOrder(original.getSortOrder() + 1);

        ViewConfig saved = viewRepo.save(copy);
        log.info("Duplicated view id={} → new id={}", viewId, saved.getId());
        return ViewConfigResponse.from(saved);
    }

    // -------------------------------------------------------------------------

    private ViewConfig findView(UUID viewId) {
        return viewRepo.findById(viewId)
                .orElseThrow(() -> new ResourceNotFoundException("ViewConfig", viewId));
    }

    private void applyRequest(ViewConfig view, CreateViewRequest req) {
        view.setName(req.name());
        view.setViewType(req.viewType());
        view.setColumns(req.columns());
        view.setSortRules(req.sortRules());
        view.setFilterRules(req.filterRules());
        view.setGroupBy(req.groupBy());
        view.setAggregations(req.aggregations());
        view.setChartConfig(req.chartConfig());
        view.setIsDefault(req.isDefault() != null && req.isDefault());
        view.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    }

    private void applyUpdate(ViewConfig view, UpdateViewRequest req) {
        if (req.name() != null) view.setName(req.name());
        if (req.viewType() != null) view.setViewType(req.viewType());
        if (req.columns() != null) view.setColumns(req.columns());
        if (req.sortRules() != null) view.setSortRules(req.sortRules());
        if (req.filterRules() != null) view.setFilterRules(req.filterRules());
        if (req.groupBy() != null) view.setGroupBy(req.groupBy());
        if (req.aggregations() != null) view.setAggregations(req.aggregations());
        if (req.chartConfig() != null) view.setChartConfig(req.chartConfig());
        if (req.sortOrder() != null) view.setSortOrder(req.sortOrder());
    }
}
