package com.thejas.ai_frms.dashboard.service;

import com.thejas.ai_frms.dashboard.dto.DashboardSummaryResponse;
import com.thejas.ai_frms.dashboard.dto.ExecutionTrendResponse;
import com.thejas.ai_frms.dashboard.dto.RuleWiseExecutionStats;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse getDashboardSummary();

    List<RuleWiseExecutionStats> getRuleWiseExecutionStats();

    List<ExecutionTrendResponse> getExecutionTrend(int days);
}