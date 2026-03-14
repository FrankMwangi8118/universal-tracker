package com.codify.universaltracker.entry.repository;

import com.codify.universaltracker.entry.entity.FieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldValueRepository extends JpaRepository<FieldValue, UUID> {

    List<FieldValue> findByEntryId(UUID entryId);

    Optional<FieldValue> findByEntryIdAndFieldDefinitionId(UUID entryId, UUID fieldDefinitionId);

    boolean existsByFieldDefinitionIdAndValueTextAndEntryIdNot(
            UUID fieldDefinitionId, String valueText, UUID excludeEntryId);

    @Modifying
    @Query("DELETE FROM FieldValue fv WHERE fv.entryId = :entryId")
    void deleteAllByEntryId(@Param("entryId") UUID entryId);
}
