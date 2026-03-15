package com.codify.universaltracker.view.repository;

import com.codify.universaltracker.view.entity.ViewConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ViewConfigRepository extends JpaRepository<ViewConfig, UUID> {

    List<ViewConfig> findByTrackerIdOrderBySortOrderAsc(UUID trackerId);

    Optional<ViewConfig> findByTrackerIdAndIsDefaultTrue(UUID trackerId);

    int countByTrackerId(UUID trackerId);

    @Modifying
    @Query("UPDATE ViewConfig v SET v.isDefault = false WHERE v.trackerId = :trackerId AND v.id <> :excludeId")
    void clearDefaultsExcept(@Param("trackerId") UUID trackerId, @Param("excludeId") UUID excludeId);
}
