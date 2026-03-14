package com.codify.universaltracker.field.repository;

import com.codify.universaltracker.field.entity.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, UUID> {

    List<FieldDefinition> findByTrackerIdAndIsActiveTrueOrderBySortOrderAsc(UUID trackerId);

    List<FieldDefinition> findByTrackerIdOrderBySortOrderAsc(UUID trackerId);

    Optional<FieldDefinition> findByTrackerIdAndSlug(UUID trackerId, String slug);

    boolean existsByTrackerIdAndSlug(UUID trackerId, String slug);

    @Query("SELECT COALESCE(MAX(f.sortOrder), -1) FROM FieldDefinition f WHERE f.trackerId = :trackerId")
    int findMaxSortOrderByTrackerId(@Param("trackerId") UUID trackerId);

    @Query("SELECT COUNT(fv) FROM FieldValue fv WHERE fv.fieldDefinitionId = :fieldId")
    long countValuesByFieldId(@Param("fieldId") UUID fieldId);
}
