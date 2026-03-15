package com.codify.universaltracker.entry.repository;

import com.codify.universaltracker.entry.entity.Entry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    @Query("SELECT e FROM Entry e WHERE e.trackerId = :trackerId AND e.deletedAt IS NULL ORDER BY e.entryDate DESC")
    Page<Entry> findActiveByTrackerId(@Param("trackerId") UUID trackerId, Pageable pageable);

    Optional<Entry> findByIdAndDeletedAtIsNull(UUID id);

    // findFiltered removed — use EntryService.findFilteredDynamic() via EntityManager

    @Modifying
    @Query("UPDATE Entry e SET e.deletedAt = :now WHERE e.id IN :ids AND e.userId = :userId")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT e FROM Entry e WHERE e.trackerId = :trackerId AND e.deletedAt IS NULL ORDER BY e.createdAt DESC")
    List<Entry> findRecentByTrackerId(@Param("trackerId") UUID trackerId, Pageable pageable);
}
