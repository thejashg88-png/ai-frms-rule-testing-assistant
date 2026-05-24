package com.thejas.ai_frms.scheduler.service;

import com.thejas.ai_frms.common.dto.PageResponse;
import com.thejas.ai_frms.common.enums.RuleStatus;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioCreateRequest;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioResponse;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioSearchRequest;
import com.thejas.ai_frms.scheduler.dto.ScheduledScenarioUpdateRequest;

public interface SchedulerService {

    ScheduledScenarioResponse createSchedule(ScheduledScenarioCreateRequest request);

    ScheduledScenarioResponse updateSchedule(Long scheduleId, ScheduledScenarioUpdateRequest request);

    ScheduledScenarioResponse getScheduleById(Long scheduleId);

    PageResponse<ScheduledScenarioResponse> searchSchedules(ScheduledScenarioSearchRequest request);

    ScheduledScenarioResponse changeScheduleStatus(Long scheduleId, RuleStatus status);

    ScheduledScenarioResponse runScheduleNow(Long scheduleId);

    void deleteSchedule(Long scheduleId);
}