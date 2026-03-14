package com.codify.universaltracker.domain.service;

import com.codify.universaltracker.common.exception.ResourceNotFoundException;
import com.codify.universaltracker.common.util.SlugUtil;
import com.codify.universaltracker.common.util.UserContext;
import com.codify.universaltracker.domain.dto.CreateDomainRequest;
import com.codify.universaltracker.domain.dto.DomainListResponse;
import com.codify.universaltracker.domain.dto.DomainResponse;
import com.codify.universaltracker.domain.dto.UpdateDomainRequest;
import com.codify.universaltracker.domain.entity.Domain;
import com.codify.universaltracker.domain.repository.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DomainService {

    private static final Logger log = LoggerFactory.getLogger(DomainService.class);

    private final DomainRepository domainRepository;

    public DomainService(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    @Transactional
    public DomainResponse createDomain(CreateDomainRequest request) {
        UUID userId = UserContext.get();

        String slug = SlugUtil.toUniqueSlug(request.name(),
                candidate -> domainRepository.existsByUserIdAndSlug(userId, candidate));

        Domain domain = new Domain();
        domain.setUserId(userId);
        domain.setName(request.name());
        domain.setSlug(slug);
        domain.setIcon(request.icon());
        domain.setColor(request.color());
        domain.setDescription(request.description());

        Domain saved = domainRepository.save(domain);
        log.info("Created domain '{}' (id={}) for user {}", saved.getName(), saved.getId(), userId);
        return DomainResponse.from(saved, 0L);
    }

    @Transactional(readOnly = true)
    public List<DomainListResponse> listDomains() {
        UUID userId = UserContext.get();
        return domainRepository.findByUserIdAndIsActiveTrueOrderBySortOrderAsc(userId)
                .stream()
                .map(d -> DomainListResponse.from(d, domainRepository.countActiveTrackersByDomainId(d.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DomainResponse getDomainBySlug(String slug) {
        UUID userId = UserContext.get();
        Domain domain = domainRepository.findByUserIdAndSlug(userId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Domain", slug));
        long trackerCount = domainRepository.countActiveTrackersByDomainId(domain.getId());
        return DomainResponse.from(domain, trackerCount);
    }

    @Transactional(readOnly = true)
    public DomainResponse getDomainById(UUID domainId) {
        Domain domain = findOwnedDomain(domainId);
        long trackerCount = domainRepository.countActiveTrackersByDomainId(domainId);
        return DomainResponse.from(domain, trackerCount);
    }

    @Transactional
    public DomainResponse updateDomain(UUID domainId, UpdateDomainRequest request) {
        Domain domain = findOwnedDomain(domainId);

        if (request.name() != null && !request.name().equals(domain.getName())) {
            UUID userId = UserContext.get();
            String newSlug = SlugUtil.toUniqueSlug(request.name(),
                    candidate -> !candidate.equals(domain.getSlug())
                            && domainRepository.existsByUserIdAndSlug(userId, candidate));
            domain.setName(request.name());
            domain.setSlug(newSlug);
        }
        if (request.icon() != null) domain.setIcon(request.icon());
        if (request.color() != null) domain.setColor(request.color());
        if (request.description() != null) domain.setDescription(request.description());
        if (request.sortOrder() != null) domain.setSortOrder(request.sortOrder());
        if (request.isActive() != null) domain.setIsActive(request.isActive());

        Domain saved = domainRepository.save(domain);
        log.info("Updated domain id={}", domainId);
        long trackerCount = domainRepository.countActiveTrackersByDomainId(domainId);
        return DomainResponse.from(saved, trackerCount);
    }

    @Transactional
    public void deleteDomain(UUID domainId) {
        Domain domain = findOwnedDomain(domainId);
        domainRepository.delete(domain);
        log.info("Deleted domain id={} (cascades to trackers, fields, entries)", domainId);
    }

    @Transactional
    public void reorderDomains(List<UUID> orderedIds) {
        UUID userId = UserContext.get();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            Domain domain = domainRepository.findById(id)
                    .filter(d -> d.getUserId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Domain", id));
            domain.setSortOrder(i);
            domainRepository.save(domain);
        }
        log.info("Reordered {} domains for user {}", orderedIds.size(), userId);
    }

    // -------------------------------------------------------------------------

    private Domain findOwnedDomain(UUID domainId) {
        UUID userId = UserContext.get();
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Domain", domainId));
        if (!domain.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Domain", domainId);
        }
        return domain;
    }
}
