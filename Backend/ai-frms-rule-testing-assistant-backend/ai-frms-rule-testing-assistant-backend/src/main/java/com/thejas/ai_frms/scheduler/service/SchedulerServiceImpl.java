package com.thejas.ai_frms.scheduler.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.common.exception.BadRequestException;
import com.thejas.ai_frms.common.exception.ResourceNotFoundException;
import com.thejas.ai_frms.execution.dto.ExecuteScenarioRequest;
import com.thejas.ai_frms.execution.dto.ExecuteTestResponse;
import com.thejas.ai_frms.execution.service.TestExecutionService;
import com.thejas.ai_frms.scenario.entity.TestScenarioEntity;
import com.thejas.ai_frms.scenario.repository.TestScenarioRepository;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioCreateRequest;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioResponse;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioSearchRequest;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioUpdateRequest;
import com.thejas.ai_frms.scheduler.entity.ScheduledScenarioEntity;
import com.thejas.ai_frms.scheduler.mapper.ScheduledScenarioMapper;
import com.thejas.ai_frms.scheduler.repository.ScheduledScenarioRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SchedulerServiceImpl implements SchedulerService {

    private final ScheduledScenarioRepository scheduledScenarioRepository;
    private final TestScenarioRepository testScenarioRepository;
    private final TestExecutionService testExecutionService;

    public SchedulerServiceImpl(
            ScheduledScenarioRepository scheduledScenarioRepository,
            TestScenarioRepository testScenarioRepository,
            TestExecutionService testExecutionService
    ) {
        this.scheduledScenarioRepository = scheduledScenarioRepository;
        this.testScenarioRepository = testScenarioRepository;
        this.testExecutionService = testExecutionService;
    }

    @Override
    @Transactional
    public ScheduledScenarioResponse createSchedule(ScheduledScenarioCreateRequest request) {
        if (scheduledScenarioRepository.existsByScheduleNameIgnoreCase(request.getScheduleName())) {
            throw new BadRequestException("Schedule name already exists: " + request.getScheduleName());
        }

        validateCronExpression(request.getCronExpression());

        TestScenarioEntity scenario = getScenarioEntity(request.getScenarioId());

        ScheduledScenarioEntity entity = ScheduledScenarioMapper.toEntity(request, scenario);
        entity.setNextRunAt(calculateNextRunTime(request.getCronExpression(), LocalDateTime.now()));

        ScheduledScenarioEntity savedEntity = scheduledScenarioRepository.save(entity);

        return ScheduledScenarioMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public ScheduledScenarioResponse updateSchedule(Long scheduleId, ScheduledScenarioUpdateRequest request) {
        ScheduledScenarioEntity entity = getScheduleEntity(scheduleId);

        if (request.getScheduleName() != null
                && scheduledScenarioRepository.existsByScheduleNameIgnoreCaseAndScheduleIdNot(
                request.getScheduleName(),
                scheduleId
        )) {
            throw new BadRequestException("Schedule name already exists: " + request.getScheduleName());
        }

        TestScenarioEntity scenario = null;

        if (request.getScenarioId() != null) {
            scenario = getScenarioEntity(request.getScenarioId());
        }

        if (request.getCronExpression() != null) {
            validateCronExpression(request.getCronExpression());
            entity.setNextRunAt(calculateNextRunTime(request.getCronExpression(), LocalDateTime.now()));
        }

        ScheduledScenarioMapper.updateEntity(entity, request, scenario);

        ScheduledScenarioEntity updatedEntity = scheduledScenarioRepository.save(entity);

        return ScheduledScenarioMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledScenarioResponse getScheduleById(Long scheduleId) {
        ScheduledScenarioEntity entity = getScheduleEntity(scheduleId);
        return ScheduledScenarioMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScheduledScenarioResponse> searchSchedules(ScheduledScenarioSearchRequest request) {
        Pageable pageable = buildPageable(request);

        Page<ScheduledScenarioResponse> responsePage = scheduledScenarioRepository
                .findAll(buildSpecification(request), pageable)
                .map(ScheduledScenarioMapper::toResponse);

        return PageResponse.fromPage(responsePage);
    }

    @Override
    @Transactional
    public ScheduledScenarioResponse changeScheduleStatus(Long scheduleId, RuleStatus status) {
        ScheduledScenarioEntity entity = getScheduleEntity(scheduleId);
        entity.setStatus(status);

        if (status == RuleStatus.ACTIVE) {
            entity.setNextRunAt(calculateNextRunTime(entity.getCronExpression(), LocalDateTime.now()));
        }

        ScheduledScenarioEntity updatedEntity = scheduledScenarioRepository.save(entity);

        return ScheduledScenarioMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public ScheduledScenarioResponse runScheduleNow(Long scheduleId) {
        ScheduledScenarioEntity entity = getScheduleEntity(scheduleId);

        ExecuteScenarioRequest request = new ExecuteScenarioRequest();
        request.setScenarioId(entity.getScenario().getScenarioId());
        request.setExecutedBy("SCHEDULER_MANUAL_RUN");

        ExecuteTestResponse executionResponse = testExecutionService.executeScenario(request);

        LocalDateTime now = LocalDateTime.now();

        entity.setLastRunAt(now);
        entity.setLastExecutionId(executionResponse.getExecutionId());
        entity.setRunCount(entity.getRunCount() == null ? 1L : entity.getRunCount() + 1);
        entity.setNextRunAt(calculateNextRunTime(entity.getCronExpression(), now));

        ScheduledScenarioEntity updatedEntity = scheduledScenarioRepository.save(entity);

        return ScheduledScenarioMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        ScheduledScenarioEntity entity = getScheduleEntity(scheduleId);
        scheduledScenarioRepository.delete(entity);
    }

    private ScheduledScenarioEntity getScheduleEntity(Long scheduleId) {
        return scheduledScenarioRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));
    }

    private TestScenarioEntity getScenarioEntity(Long scenarioId) {
        return testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario not found with id: " + scenarioId));
    }

    private void validateCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new BadRequestException("Cron expression is required");
        }

        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new BadRequestException("Invalid cron expression: " + cronExpression);
        }
    }

    private LocalDateTime calculateNextRunTime(String cronExpression, LocalDateTime fromTime) {
        CronExpression expression = CronExpression.parse(cronExpression);
        return expression.next(fromTime);
    }

    private Pageable buildPageable(ScheduledScenarioSearchRequest request) {
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 10 : request.getSize();

        String sortBy = request.getSortBy() == null || request.getSortBy().isBlank()
                ? "createdAt"
                : request.getSortBy();

        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private Specification<ScheduledScenarioEntity> buildSpecification(ScheduledScenarioSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getScheduleName() != null && !request.getScheduleName().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("scheduleName")),
                        "%" + request.getScheduleName().toLowerCase() + "%"
                ));
            }

            if (request.getScenarioId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("scenario").get("scenarioId"), request.getScenarioId()));
            }

            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}