package com.thejas.ai_frms.scheduler.repository;

import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.scheduler.entity.ScheduledScenarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledScenarioRepository extends JpaRepository<ScheduledScenarioEntity, Long>,
        JpaSpecificationExecutor<ScheduledScenarioEntity> {

    boolean existsByScheduleNameIgnoreCase(String scheduleName);

    boolean existsByScheduleNameIgnoreCaseAndScheduleIdNot(String scheduleName, Long scheduleId);

    List<ScheduledScenarioEntity> findByStatusAndNextRunAtLessThanEqual(
            RuleStatus status,
            LocalDateTime nextRunAt
    );
}