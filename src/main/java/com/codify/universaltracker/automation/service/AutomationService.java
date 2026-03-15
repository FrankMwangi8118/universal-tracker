package com.codify.universaltracker.automation.service;

import com.codify.universaltracker.automation.dto.*;
import com.codify.universaltracker.automation.entity.Automation;
import com.codify.universaltracker.automation.entity.AutomationLog;
import com.codify.universaltracker.automation.repository.AutomationLogRepository;
import com.codify.universaltracker.automation.repository.AutomationRepository;
import com.codify.universaltracker.common.exception.ResourceNotFoundException;
import com.codify.universaltracker.tracker.service.TrackerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AutomationService {

    private final AutomationRepository automationRepository;
    private final AutomationLogRepository logRepository;
    private final TrackerService trackerService;

    public AutomationService(AutomationRepository automationRepository,
                             AutomationLogRepository logRepository,
                             TrackerService trackerService) {
        this.automationRepository = automationRepository;
        this.logRepository = logRepository;
        this.trackerService = trackerService;
    }

    @Transactional
    public AutomationResponse create(UUID trackerId, CreateAutomationRequest req) {
        trackerService.findOwnedTracker(trackerId); // ownership check

        Automation automation = new Automation();
        automation.setTrackerId(trackerId);
        automation.setName(req.name());
        automation.setDescription(req.description());
        automation.setTriggerEvent(req.triggerEvent());
        automation.setTriggerConfig(req.triggerConfig());
        automation.setConditions(req.conditions());
        automation.setActionType(req.actionType());
        automation.setActionParams(req.actionParams());
        automation.setIsActive(true);
        automation.setRunCount(0);

        return toResponse(automationRepository.save(automation));
    }

    @Transactional(readOnly = true)
    public List<AutomationResponse> list(UUID trackerId) {
        trackerService.findOwnedTracker(trackerId);
        return automationRepository.findByTrackerIdOrderByCreatedAtAsc(trackerId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AutomationResponse get(UUID id) {
        return toResponse(findOwned(id));
    }

    @Transactional
    public AutomationResponse update(UUID id, UpdateAutomationRequest req) {
        Automation automation = findOwned(id);

        if (req.name() != null) automation.setName(req.name());
        if (req.description() != null) automation.setDescription(req.description());
        if (req.triggerEvent() != null) automation.setTriggerEvent(req.triggerEvent());
        if (req.triggerConfig() != null) automation.setTriggerConfig(req.triggerConfig());
        if (req.conditions() != null) automation.setConditions(req.conditions());
        if (req.actionType() != null) automation.setActionType(req.actionType());
        if (req.actionParams() != null) automation.setActionParams(req.actionParams());
        if (req.isActive() != null) automation.setIsActive(req.isActive());

        return toResponse(automationRepository.save(automation));
    }

    @Transactional
    public void delete(UUID id) {
        Automation automation = findOwned(id);
        automationRepository.delete(automation);
    }

    @Transactional
    public AutomationResponse toggle(UUID id) {
        Automation automation = findOwned(id);
        automation.setIsActive(!Boolean.TRUE.equals(automation.getIsActive()));
        return toResponse(automationRepository.save(automation));
    }

    @Transactional(readOnly = true)
    public Page<AutomationLogResponse> getLogs(UUID id, int page, int size) {
        findOwned(id); // ownership check
        return logRepository.findByAutomationIdOrderByExecutedAtDesc(id, PageRequest.of(page, size))
                .map(this::toLogResponse);
    }

    // -------------------------------------------------------------------------

    private Automation findOwned(UUID id) {
        Automation automation = automationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Automation", id));
        // Verify tracker ownership through TrackerService
        trackerService.findOwnedTracker(automation.getTrackerId());
        return automation;
    }

    AutomationResponse toResponse(Automation a) {
        return new AutomationResponse(
                a.getId(),
                a.getTrackerId(),
                a.getName(),
                a.getDescription(),
                a.getTriggerEvent(),
                a.getTriggerConfig(),
                a.getConditions(),
                a.getActionType(),
                a.getActionParams(),
                a.getIsActive(),
                a.getLastTriggered(),
                a.getRunCount(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    AutomationLogResponse toLogResponse(AutomationLog log) {
        return new AutomationLogResponse(
                log.getId(),
                log.getAutomationId(),
                log.getEntryId(),
                log.getTriggerData(),
                log.getActionResult(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getExecutedAt()
        );
    }
}
