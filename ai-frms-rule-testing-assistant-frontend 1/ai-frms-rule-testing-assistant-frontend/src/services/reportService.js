import reportApi from '../api/reportApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms))

const mockExecutionReport = {
  totalExecutions: 47,
  passed: 38,
  failed: 9,
  passRate: 80.85,
  byStatus: [
    { status: 'PASSED', count: 38 },
    { status: 'FAILED', count: 9 },
  ],
  byType: [
    { type: 'TEST_CASE', count: 35 },
    { type: 'SCENARIO', count: 12 },
  ],
  trend: [
    { date: '2025-01-09', passed: 4, failed: 2 },
    { date: '2025-01-10', passed: 6, failed: 1 },
    { date: '2025-01-11', passed: 5, failed: 0 },
    { date: '2025-01-12', passed: 7, failed: 2 },
    { date: '2025-01-13', passed: 6, failed: 1 },
    { date: '2025-01-14', passed: 5, failed: 2 },
    { date: '2025-01-15', passed: 5, failed: 1 },
  ],
}

const mockRuleReport = {
  totalRules: 7,
  activeRules: 6,
  inactiveRules: 1,
  byType: [
    { type: 'CREDIT', count: 1, activeCount: 1 },
    { type: 'AMOUNT', count: 1, activeCount: 1 },
    { type: 'VELOCITY', count: 1, activeCount: 1 },
    { type: 'GEO', count: 1, activeCount: 1 },
    { type: 'FRAUD', count: 1, activeCount: 1 },
    { type: 'CARD', count: 1, activeCount: 0 },
    { type: 'FREQUENCY', count: 1, activeCount: 1 },
  ],
  byAction: [
    { action: 'REJECT', count: 4 },
    { action: 'MONITOR', count: 3 },
  ],
}

export const reportService = {
  getExecutionReport: async (params = {}) => {
    if (isMock) { await delay(); return mockExecutionReport }
    try {
      const resp = await reportApi.getExecutionReport(params)
      return resp?.data ?? resp
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  getRuleReport: async (params = {}) => {
    if (isMock) { await delay(); return mockRuleReport }
    try {
      const resp = await reportApi.getRuleReport(params)
      return resp?.data ?? resp
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  downloadReport: async (type, params = {}) => {
    if (isMock) {
      await delay(800)
      const content = `FRMS Report - ${type.toUpperCase()}\nGenerated: ${new Date().toISOString()}\n\nMock data report for ${type}.\n`
      const blob = new Blob([content], { type: 'text/csv' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${type}-report.csv`
      a.click()
      URL.revokeObjectURL(url)
      return true
    }
    try {
      const blob = await reportApi.downloadReport(type, params)
      const filename = type === 'rules' ? 'rules-report.csv' : 'executions-report.csv'
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
      return true
    } catch (err) {
      const responseData = err?.response?.data
      if (responseData instanceof Blob) {
        let message = 'Report download failed'
        try {
          const text = await responseData.text()
          const json = JSON.parse(text)
          console.error('[Report Download Error Body]', json)
          message = json.message || message
        } catch (parseErr) {
          console.warn('[Report Download] Could not parse error response', parseErr)
        }
        throw new Error(`Failed to download report: ${message}`)
      }
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },
}

export default reportService
