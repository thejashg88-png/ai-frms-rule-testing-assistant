package com.thejas.ai_frms.scheduler.controller;

import com.thejas.ai_frms.common.constants.ApiPathConstants;
import com.thejas.ai_frms.common.dto.ApiResponse;
import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioCreateRequest;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioResponse;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioSearchRequest;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioUpdateRequest;
import com.thejas.ai_frms.scheduler.service.SchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPathConstants.SCHEDULER)
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledScenarioResponse>> createSchedule(
            @Valid @RequestBody ScheduledScenarioCreateRequest request
    ) {
        ScheduledScenarioResponse response = schedulerService.createSchedule(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Schedule created successfully", response));
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduledScenarioResponse>> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduledScenarioUpdateRequest request
    ) {
        ScheduledScenarioResponse response = schedulerService.updateSchedule(scheduleId, request);

        return ResponseEntity.ok(ApiResponse.success("Schedule updated successfully", response));
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduledScenarioResponse>> getScheduleById(
            @PathVariable Long scheduleId
    ) {
        ScheduledScenarioResponse response = schedulerService.getScheduleById(scheduleId);

        return ResponseEntity.ok(ApiResponse.success("Schedule fetched successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ScheduledScenarioResponse>>> searchSchedules(
            @ModelAttribute ScheduledScenarioSearchRequest request
    ) {
        PageResponse<ScheduledScenarioResponse> response = schedulerService.searchSchedules(request);

        return ResponseEntity.ok(ApiResponse.success("Schedules fetched successfully", response));
    }

    @PatchMapping("/{scheduleId}/status")
    public ResponseEntity<ApiResponse<ScheduledScenarioResponse>> changeScheduleStatus(
            @PathVariable Long scheduleId,
            @RequestParam RuleStatus status
    ) {
        ScheduledScenarioResponse response = schedulerService.changeScheduleStatus(scheduleId, status);

        return ResponseEntity.ok(ApiResponse.success("Schedule status changed successfully", response));
    }

    @PostMapping("/{scheduleId}/run-now")
    public ResponseEntity<ApiResponse<ScheduledScenarioResponse>> runScheduleNow(
            @PathVariable Long scheduleId
    ) {
        ScheduledScenarioResponse response = schedulerService.runScheduleNow(scheduleId);

        return ResponseEntity.ok(ApiResponse.success("Schedule executed successfully", response));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long scheduleId) {
        schedulerService.deleteSchedule(scheduleId);

        return ResponseEntity.ok(ApiResponse.success("Schedule deleted successfully"));
    }
}