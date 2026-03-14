package com.codify.universaltracker.tracker.service;

import com.codify.universaltracker.common.exception.BusinessRuleException;
import com.codify.universaltracker.common.exception.ResourceNotFoundException;
import com.codify.universaltracker.common.util.SlugUtil;
import com.codify.universaltracker.common.util.UserContext;
import com.codify.universaltracker.domain.entity.Domain;
import com.codify.universaltracker.domain.repository.DomainRepository;
import com.codify.universaltracker.tracker.dto.CreateTrackerRequest;
import com.codify.universaltracker.tracker.dto.TrackerListResponse;
import com.codify.universaltracker.tracker.dto.TrackerResponse;
import com.codify.universaltracker.tracker.dto.UpdateTrackerRequest;
import com.codify.universaltracker.tracker.entity.Tracker;
import com.codify.universaltracker.tracker.repository.TrackerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TrackerService {

    private static final Logger log = LoggerFactory.getLogger(TrackerService.class);

    private final TrackerRepository trackerRepository;
    private final DomainRepository domainRepository;

    public TrackerService(TrackerRepository trackerRepository, DomainRepository domainRepository) {
        this.trackerRepository = trackerRepository;
        this.domainRepository = domainRepository;
    }

    @Transactional
    public TrackerResponse createTracker(CreateTrackerRequest request) {
        UUID userId = UserContext.get();

        Domain domain = domainRepository.findById(request.domainId())
                .orElseThrow(() -> new ResourceNotFoundException("Domain", request.domainId()));
        if (!domain.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Domain", request.domainId());
        }

        String slug = SlugUtil.toUniqueSlug(request.name(),
                candidate -> trackerRepository.existsByDomainIdAndSlug(request.domainId(), candidate));

        Tracker tracker = new Tracker();
        tracker.setDomainId(request.domainId());
        tracker.setUserId(userId);
        tracker.setName(request.name());
        tracker.setSlug(slug);
        tracker.setIcon(request.icon());
        tracker.setDescription(request.description());
        if (request.entryNameSingular() != null) tracker.setEntryNameSingular(request.entryNameSingular());
        if (request.entryNamePlural() != null) tracker.setEntryNamePlural(request.entryNamePlural());

        Tracker saved = trackerRepository.save(tracker);
        log.info("Created tracker '{}' (id={}) in domain {}", saved.getName(), saved.getId(), request.domainId());
        return TrackerResponse.from(saved, 0L, 0L, null);
    }

    @Transactional(readOnly = true)
    public List<TrackerListResponse> listTrackersByDomain(String domainSlug) {
        UUID userId = UserContext.get();
        Domain domain = domainRepository.findByUserIdAndSlug(userId, domainSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Domain", domainSlug));

        return trackerRepository.findByDomainIdAndIsActiveTrueOrderBySortOrderAsc(domain.getId())
                .stream()
                .map(t -> TrackerListResponse.from(t,
                        trackerRepository.countActiveFieldsByTrackerId(t.getId()),
                        trackerRepository.countEntriesByTrackerId(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrackerListResponse> listAllTrackers() {
        UUID userId = UserContext.get();
        return trackerRepository.findByUserIdAndIsActiveTrueOrderByDomainIdAscSortOrderAsc(userId)
                .stream()
                .map(t -> TrackerListResponse.from(t,
                        trackerRepository.countActiveFieldsByTrackerId(t.getId()),
                        trackerRepository.countEntriesByTrackerId(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackerResponse getTracker(UUID trackerId) {
        Tracker tracker = findOwnedTracker(trackerId);
        long fieldCount = trackerRepository.countActiveFieldsByTrackerId(trackerId);
        long entryCount = trackerRepository.countEntriesByTrackerId(trackerId);
        Instant lastEntry = trackerRepository.findLastEntryDateByTrackerId(trackerId).orElse(null);
        return TrackerResponse.from(tracker, fieldCount, entryCount, lastEntry);
    }

    @Transactional
    public TrackerResponse updateTracker(UUID trackerId, UpdateTrackerRequest request) {
        Tracker tracker = findOwnedTracker(trackerId);

        if (request.name() != null && !request.name().equals(tracker.getName())) {
            String newSlug = SlugUtil.toUniqueSlug(request.name(),
                    candidate -> !candidate.equals(tracker.getSlug())
                            && trackerRepository.existsByDomainIdAndSlug(tracker.getDomainId(), candidate));
            tracker.setName(request.name());
            tracker.setSlug(newSlug);
        }
        if (request.icon() != null) tracker.setIcon(request.icon());
        if (request.description() != null) tracker.setDescription(request.description());
        if (request.entryNameSingular() != null) tracker.setEntryNameSingular(request.entryNameSingular());
        if (request.entryNamePlural() != null) tracker.setEntryNamePlural(request.entryNamePlural());
        if (request.summaryConfig() != null) tracker.setSummaryConfig(request.summaryConfig());
        if (request.sortOrder() != null) tracker.setSortOrder(request.sortOrder());
        if (request.isActive() != null) tracker.setIsActive(request.isActive());
        if (request.defaultDateField() != null) tracker.setDefaultDateField(request.defaultDateField());

        Tracker saved = trackerRepository.save(tracker);
        log.info("Updated tracker id={}", trackerId);
        long fieldCount = trackerRepository.countActiveFieldsByTrackerId(trackerId);
        long entryCount = trackerRepository.countEntriesByTrackerId(trackerId);
        Instant lastEntry = trackerRepository.findLastEntryDateByTrackerId(trackerId).orElse(null);
        return TrackerResponse.from(saved, fieldCount, entryCount, lastEntry);
    }

    @Transactional
    public void deleteTracker(UUID trackerId) {
        Tracker tracker = findOwnedTracker(trackerId);
        trackerRepository.delete(tracker);
        log.info("Deleted tracker id={} (cascades to fields, entries, views, automations)", trackerId);
    }

    @Transactional
    public void reorderTrackers(UUID domainId, List<UUID> orderedIds) {
        UUID userId = UserContext.get();
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain", domainId));
        if (!domain.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Domain", domainId);
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            Tracker tracker = trackerRepository.findById(id)
                    .filter(t -> t.getDomainId().equals(domainId))
                    .orElseThrow(() -> new ResourceNotFoundException("Tracker", id));
            if (!tracker.getUserId().equals(userId)) {
                throw new BusinessRuleException("Tracker " + id + " does not belong to current user");
            }
            tracker.setSortOrder(i);
            trackerRepository.save(tracker);
        }
    }

    // -------------------------------------------------------------------------

    public Tracker findOwnedTracker(UUID trackerId) {
        UUID userId = UserContext.get();
        Tracker tracker = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracker", trackerId));
        if (!tracker.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Tracker", trackerId);
        }
        return tracker;
    }
}
