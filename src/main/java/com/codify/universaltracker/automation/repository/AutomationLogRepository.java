package com.codify.universaltracker.automation.repository;

import com.codify.universaltracker.automation.entity.AutomationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AutomationLogRepository extends JpaRepository<AutomationLog, UUID> {

    Page<AutomationLog> findByAutomationIdOrderByExecutedAtDesc(UUID automationId, Pageable pageable);
}
