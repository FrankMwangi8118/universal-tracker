package com.codify.universaltracker.tracker.repository;

import com.codify.universaltracker.tracker.entity.Tracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackerRepository extends JpaRepository<Tracker, UUID> {

    List<Tracker> findByDomainIdAndIsActiveTrueOrderBySortOrderAsc(UUID domainId);

    Optional<Tracker> findByDomainIdAndSlug(UUID domainId, String slug);

    List<Tracker> findByUserIdAndIsActiveTrueOrderByDomainIdAscSortOrderAsc(UUID userId);

    boolean existsByDomainIdAndSlug(UUID domainId, String slug);

    @Query("SELECT COUNT(f) FROM FieldDefinition f WHERE f.trackerId = :trackerId AND f.isActive = true")
    long countActiveFieldsByTrackerId(@Param("trackerId") UUID trackerId);

    @Query("SELECT COUNT(e) FROM Entry e WHERE e.trackerId = :trackerId AND e.deletedAt IS NULL")
    long countEntriesByTrackerId(@Param("trackerId") UUID trackerId);

    @Query("SELECT MAX(e.entryDate) FROM Entry e WHERE e.trackerId = :trackerId AND e.deletedAt IS NULL")
    Optional<Instant> findLastEntryDateByTrackerId(@Param("trackerId") UUID trackerId);
}
