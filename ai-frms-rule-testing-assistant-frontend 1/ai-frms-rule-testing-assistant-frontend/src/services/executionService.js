import executionApi from '../api/executionApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

let nextId = 30
const mockStore = [
  {
    id: 1, executionType: 'TEST_CASE', entityId: 1, entityName: 'Credit limit exceeded — should REJECT',
    status: 'PASSED', result: 'REJECT', failureReason: null,
    executedAt: '2025-01-15T11:00:00', durationMs: 152,
  },
  {
    id: 2, executionType: 'TEST_CASE', entityId: 2, entityName: 'Amount within limit — should ACCEPT',
    status: 'PASSED', result: 'ACCEPT', failureReason: null,
    executedAt: '2025-01-15T11:05:00', durationMs: 98,
  },
  {
    id: 3, executionType: 'TEST_CASE', entityId: 3, entityName: 'High value alert — should MONITOR',
    status: 'FAILED', result: 'REJECT',
    failureReason: 'Expected MONITOR but rule returned REJECT. Rule threshold mismatch: maxAmount=50000, txnAmount=55000',
    executedAt: '2025-01-15T11:10:00', durationMs: 201,
  },
  {
    id: 4, executionType: 'SCENARIO', entityId: 2, entityName: 'Card Velocity Scenario',
    status: 'PASSED', result: null, failureReason: null,
    executedAt: '2025-01-14T15:30:00', durationMs: 480,
  },
  {
    id: 5, executionType: 'TEST_CASE', entityId: 4, entityName: 'Velocity breach — 12 txns in 1 hour',
    status: 'PASSED', result: 'REJECT', failureReason: null,
    executedAt: '2025-01-14T15:25:00', durationMs: 175,
  },
  {
    id: 6, executionType: 'TEST_CASE', entityId: 5, entityName: 'Geo mismatch — card issued in US, used in EU',
    status: 'PASSED', result: 'MONITOR', failureReason: null,
    executedAt: '2025-01-13T09:00:00', durationMs: 133,
  },
  {
    id: 7, executionType: 'SCENARIO', entityId: 1, entityName: 'High Value Transaction Tests',
    status: 'FAILED', result: null,
    failureReason: '1 of 5 test cases failed. See individual results for details.',
    executedAt: '2025-01-12T14:00:00', durationMs: 920,
  },
]

const applyFilters = (data, params) => {
  let result = [...data]
  if (params.status)        result = result.filter((e) => e.status === params.status)
  if (params.executionType) result = result.filter((e) => e.executionType === params.executionType)
  if (params.search) {
    const q = params.search.toLowerCase()
    result = result.filter((e) => e.entityName.toLowerCase().includes(q))
  }
  return result
}

const ACTIONS = ['ACCEPT', 'MONITOR', 'REJECT']
const STATUSES = ['PASSED', 'FAILED', 'PASSED', 'PASSED']

export const executionService = {
  getAll: async (params = {}) => {
    if (isMock) { await delay(); return applyFilters(mockStore, params) }
    try { return await executionApi.getAll(params) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  getById: async (id) => {
    if (isMock) {
      await delay()
      const e = mockStore.find((r) => r.id === Number(id))
      if (!e) throw new Error('Execution not found')
      return e
    }
    try { return await executionApi.getById(id) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  runTestCase: async (testCaseId, testCase) => {
    if (isMock) {
      await delay(1200)
      const status = STATUSES[Math.floor(Math.random() * STATUSES.length)]
      const result = ACTIONS[Math.floor(Math.random() * ACTIONS.length)]
      const execution = {
        id: nextId++,
        executionType: 'TEST_CASE',
        entityId: Number(testCaseId),
        entityName: testCase?.name ?? `Test Case #${testCaseId}`,
        status,
        result,
        failureReason: status === 'FAILED' ? 'Mock: Expected action did not match rule output' : null,
        executedAt: new Date().toISOString(),
        durationMs: Math.floor(Math.random() * 300) + 50,
      }
      mockStore.unshift(execution)
      return execution
    }
    try { return await executionApi.runTestCase(testCaseId) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  runScenario: async (scenarioId, scenario) => {
    if (isMock) {
      await delay(2000)
      const status = Math.random() > 0.3 ? 'PASSED' : 'FAILED'
      const execution = {
        id: nextId++,
        executionType: 'SCENARIO',
        entityId: Number(scenarioId),
        entityName: scenario?.name ?? `Scenario #${scenarioId}`,
        status,
        result: null,
        failureReason: status === 'FAILED' ? 'Mock: One or more test cases failed in this scenario' : null,
        executedAt: new Date().toISOString(),
        durationMs: Math.floor(Math.random() * 1000) + 500,
      }
      mockStore.unshift(execution)
      return execution
    }
    try { return await executionApi.runScenario(scenarioId) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },
}

export default executionService
