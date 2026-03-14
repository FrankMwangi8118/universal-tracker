package com.codify.universaltracker.domain.repository;

import com.codify.universaltracker.domain.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainRepository extends JpaRepository<Domain, UUID> {

    List<Domain> findByUserIdAndIsActiveTrueOrderBySortOrderAsc(UUID userId);

    Optional<Domain> findByUserIdAndSlug(UUID userId, String slug);

    boolean existsByUserIdAndSlug(UUID userId, String slug);

    @Query("SELECT COUNT(t) FROM Tracker t WHERE t.domainId = :domainId AND t.isActive = true")
    long countActiveTrackersByDomainId(@Param("domainId") UUID domainId);
}
