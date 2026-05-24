package com.thejas.ai_frms.scheduler.service;

import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.execution.dto.ExecuteScenarioRequest;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import com.thejas.ai_frms.execution.service.TestExecutionService;
import com.thejas.ai_frms.scheduler.entity.ScheduledScenarioEntity;
import com.thejas.ai_frms.scheduler.repository.ScheduledScenarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledScenarioRunner {

    private final ScheduledScenarioRepository scheduledScenarioRepository;
    private final TestExecutionService testExecutionService;

    public ScheduledScenarioRunner(
            ScheduledScenarioRepository scheduledScenarioRepository,
            TestExecutionService testExecutionService
    ) {
        this.scheduledScenarioRepository = scheduledScenarioRepository;
        this.testExecutionService = testExecutionService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.poll-interval-ms:60000}")
    @Transactional
    public void runDueSchedules() {
        LocalDateTime now = LocalDateTime.now();

        List<ScheduledScenarioEntity> dueSchedules =
                scheduledScenarioRepository.findByStatusAndNextRunAtLessThanEqual(RuleStatus.ACTIVE, now);

        for (ScheduledScenarioEntity schedule : dueSchedules) {
            runSingleSchedule(schedule, now);
        }
    }

    private void runSingleSchedule(ScheduledScenarioEntity schedule, LocalDateTime now) {
        try {
            ExecuteScenarioRequest request = new ExecuteScenarioRequest();
            request.setScenarioId(schedule.getScenario().getScenarioId());
            request.setExecutedBy("SCHEDULER");

            ExecuteTestResponse response = testExecutionService.executeScenario(request);

            schedule.setLastRunAt(now);
            schedule.setLastExecutionId(response.getExecutionId());
            schedule.setRunCount(schedule.getRunCount() == null ? 1L : schedule.getRunCount() + 1);
            schedule.setNextRunAt(calculateNextRunTime(schedule.getCronExpression(), now));

            scheduledScenarioRepository.save(schedule);
        } catch (Exception exception) {
            schedule.setLastRunAt(now);
            schedule.setNextRunAt(calculateNextRunTime(schedule.getCronExpression(), now));
            scheduledScenarioRepository.save(schedule);
        }
    }

    private LocalDateTime calculateNextRunTime(String cronExpression, LocalDateTime fromTime) {
        CronExpression expression = CronExpression.parse(cronExpression);
        return expression.next(fromTime);
    }
}