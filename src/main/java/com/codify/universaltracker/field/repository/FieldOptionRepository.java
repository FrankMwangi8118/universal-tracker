package com.codify.universaltracker.field.repository;

import com.codify.universaltracker.field.entity.FieldOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldOptionRepository extends JpaRepository<FieldOption, UUID> {

    List<FieldOption> findByFieldDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(UUID fieldDefinitionId);

    List<FieldOption> findByFieldDefinitionIdOrderBySortOrderAsc(UUID fieldDefinitionId);

    boolean existsByFieldDefinitionIdAndValue(UUID fieldDefinitionId, String value);

    Optional<FieldOption> findByFieldDefinitionIdAndValue(UUID fieldDefinitionId, String value);

    void deleteAllByFieldDefinitionId(UUID fieldDefinitionId);

    int countByFieldDefinitionId(UUID fieldDefinitionId);
}
