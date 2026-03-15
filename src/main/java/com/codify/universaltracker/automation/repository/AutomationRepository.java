package com.codify.universaltracker.automation.repository;

import com.codify.universaltracker.automation.entity.Automation;
import com.codify.universaltracker.automation.enums.AutomationTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AutomationRepository extends JpaRepository<Automation, UUID> {

    List<Automation> findByTrackerIdOrderByCreatedAtAsc(UUID trackerId);

    List<Automation> findByTrackerIdAndIsActiveTrueAndTriggerEvent(UUID trackerId, AutomationTrigger triggerEvent);
}
