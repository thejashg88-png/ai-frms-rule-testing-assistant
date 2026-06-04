import dashboardApi from '../api/dashboardApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms))

const mockSummary = {
  totalRules: 7,
  activeRules: 6,
  totalTestCases: 6,
  totalScenarios: 5,
  totalExecutions: 47,
  passedExecutions: 38,
  failedExecutions: 9,
  passRate: 80.85,
  mostFailedRuleType: 'HIGH_FREQ_TXN',
  mostTriggeredRule: 'High Value Single Transaction',
  passFailDistribution: { PASSED: 38, FAILED: 9 },
  executionsByRuleType: { HIGH_FREQ_TXN: 12, UNUSUAL_AMT: 8, TXN_VELOCITY: 7, STRUCTURING: 5, SINGLE_LARGE_TX: 4 },
  riskActionDistribution: { ACCEPT: 22, MONITOR: 15, REJECT: 10 },
  transactionStatusDistribution: { APPROVED: 30, DECLINED: 12, PENDING: 5 },
}

const mockRecentExecutions = [
  { id: 1, executionType: 'TEST_CASE', entityName: 'Credit limit exceeded — should REJECT', status: 'PASSED', executedAt: '2025-05-24T11:00:00', durationMs: 152 },
  { id: 2, executionType: 'TEST_CASE', entityName: 'High value alert — should MONITOR', status: 'FAILED', executedAt: '2025-05-24T11:10:00', durationMs: 201 },
  { id: 3, executionType: 'SCENARIO', entityName: 'Card Velocity Scenario', status: 'PASSED', executedAt: '2025-05-23T15:30:00', durationMs: 480 },
  { id: 4, executionType: 'TEST_CASE', entityName: 'Velocity breach — 12 txns in 1 hour', status: 'PASSED', executedAt: '2025-05-23T15:25:00', durationMs: 175 },
  { id: 5, executionType: 'TEST_CASE', entityName: 'Geo mismatch — card issued in US, used in EU', status: 'PASSED', executedAt: '2025-05-22T09:00:00', durationMs: 133 },
  { id: 6, executionType: 'SCENARIO', entityName: 'High Value Transaction Tests', status: 'FAILED', executedAt: '2025-05-22T14:00:00', durationMs: 920 },
  { id: 7, executionType: 'TEST_CASE', entityName: 'Duplicate transaction within same session', status: 'PASSED', executedAt: '2025-05-21T10:00:00', durationMs: 98 },
]

const mapExecution = (e) => ({
  ...e,
  id: e.executionId ?? e.id,
  status: e.executionStatus != null ? String(e.executionStatus) : (e.status ?? null),
  entityName: e.testCaseName ?? e.scenarioName ?? e.entityName,
  executedAt: e.startedAt ?? e.executedAt,
})

export const dashboardService = {
  getSummary: async () => {
    if (isMock) { await delay(); return mockSummary }
    try {
      const resp = await dashboardApi.getSummary()
      // Handles backend shapes: data.data, data, or raw object.
      // totalScenarios defaults to 0 if absent because some backend versions omit it.
      const raw = resp?.data?.data ?? resp?.data ?? resp
      return {
        totalRules:                  raw?.totalRules                  ?? 0,
        activeRules:                 raw?.activeRules                 ?? 0,
        totalScenarios:              raw?.totalScenarios              ?? 0,
        totalTestCases:              raw?.totalTestCases              ?? 0,
        totalExecutions:             raw?.totalExecutions             ?? 0,
        passedExecutions:            raw?.passedExecutions            ?? 0,
        failedExecutions:            raw?.failedExecutions            ?? 0,
        passRate:                    raw?.passRate                    ?? 0,
        mostFailedRuleType:          raw?.mostFailedRuleType          || 'N/A',
        mostTriggeredRule:           raw?.mostTriggeredRule           || 'N/A',
        passFailDistribution:        raw?.passFailDistribution        || {},
        executionsByRuleType:        raw?.executionsByRuleType        || {},
        riskActionDistribution:      raw?.riskActionDistribution      || {},
        transactionStatusDistribution: raw?.transactionStatusDistribution || {},
      }
    } catch (err) {
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },

  getRecentExecutions: async (limit = 7) => {
    if (isMock) { await delay(); return mockRecentExecutions.slice(0, limit) }
    try {
      const resp = await dashboardApi.getRecentExecutions(limit)
      // Spring may return a Page object with .content or a plain array
      const items = Array.isArray(resp?.data) ? resp.data : (resp?.data?.content ?? [])
      return items.map(mapExecution)
    } catch (err) {
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },
}

export default dashboardService
