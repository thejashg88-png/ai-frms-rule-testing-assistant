import testCaseApi from '../api/testCaseApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

let nextId = 20
const mockStore = [
  {
    id: 1, name: 'Credit limit exceeded — should REJECT',
    description: 'Amount exceeds credit limit by 50%',
    scenarioId: 1, scenarioName: 'High Value Transaction Tests',
    ruleId: 1, ruleName: 'Credit Limit Check',
    inputData: { amount: 15000, cardNumber: '4532015112830366', merchantId: 'M001', transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US' },
    expectedResult: 'FAIL', expectedAction: 'REJECT', status: 'ACTIVE',
    lastExecutionStatus: 'PASSED', createdAt: '2025-01-15',
  },
  {
    id: 2, name: 'Amount within limit — should ACCEPT',
    description: 'Transaction within normal credit range',
    scenarioId: 1, scenarioName: 'High Value Transaction Tests',
    ruleId: 1, ruleName: 'Credit Limit Check',
    inputData: { amount: 500, cardNumber: '5425233430109903', merchantId: 'M002', transactionType: 'PURCHASE', channel: 'POS', country: 'US' },
    expectedResult: 'PASS', expectedAction: 'ACCEPT', status: 'ACTIVE',
    lastExecutionStatus: 'PASSED', createdAt: '2025-01-15',
  },
  {
    id: 3, name: 'High value alert — should MONITOR',
    description: 'Transaction triggers high-value monitoring alert',
    scenarioId: 1, scenarioName: 'High Value Transaction Tests',
    ruleId: 2, ruleName: 'High Value TX Alert',
    inputData: { amount: 55000, cardNumber: '4916338506082832', merchantId: 'M003', transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US' },
    expectedResult: 'PASS', expectedAction: 'MONITOR', status: 'ACTIVE',
    lastExecutionStatus: 'FAILED', createdAt: '2025-01-14',
  },
  {
    id: 4, name: 'Velocity breach — 12 txns in 1 hour',
    description: 'Card used 12 times in rolling window, exceeds velocity rule',
    scenarioId: 2, scenarioName: 'Card Velocity Scenario',
    ruleId: 3, ruleName: 'Card Velocity Rule',
    inputData: { amount: 200, cardNumber: '4532015112830366', merchantId: 'M005', transactionType: 'PURCHASE', channel: 'POS', country: 'US' },
    expectedResult: 'FAIL', expectedAction: 'REJECT', status: 'ACTIVE',
    lastExecutionStatus: 'PASSED', createdAt: '2025-01-14',
  },
  {
    id: 5, name: 'Geo mismatch — card issued in US, used in EU',
    description: 'Card issued in US being used in France without prior travel notice',
    scenarioId: 3, scenarioName: 'Geo Mismatch Detection',
    ruleId: 4, ruleName: 'Geo Mismatch Detect',
    inputData: { amount: 350, cardNumber: '5425233430109903', merchantId: 'M004', transactionType: 'PURCHASE', channel: 'POS', country: 'FR' },
    expectedResult: 'PASS', expectedAction: 'MONITOR', status: 'ACTIVE',
    lastExecutionStatus: 'PASSED', createdAt: '2025-01-13',
  },
  {
    id: 6, name: 'Duplicate transaction within same session',
    description: 'Same card, merchant and amount submitted twice in 60 seconds',
    scenarioId: 4, scenarioName: 'Duplicate Transaction Prevention',
    ruleId: 5, ruleName: 'Duplicate TXN Check',
    inputData: { amount: 120, cardNumber: '4916338506082832', merchantId: 'M001', transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US' },
    expectedResult: 'FAIL', expectedAction: 'REJECT', status: 'ACTIVE',
    lastExecutionStatus: null, createdAt: '2025-01-12',
  },
]

const applyFilters = (data, params) => {
  let result = [...data]
  if (params.scenarioId) result = result.filter((t) => t.scenarioId === Number(params.scenarioId))
  if (params.ruleId)     result = result.filter((t) => t.ruleId === Number(params.ruleId))
  if (params.status)     result = result.filter((t) => t.status === params.status)
  if (params.search) {
    const q = params.search.toLowerCase()
    result = result.filter(
      (t) => t.name.toLowerCase().includes(q) || (t.description || '').toLowerCase().includes(q)
    )
  }
  return result
}

export const testCaseService = {
  getAll: async (params = {}) => {
    if (isMock) { await delay(); return applyFilters(mockStore, params) }
    try { return await testCaseApi.getAll(params) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  getById: async (id) => {
    if (isMock) {
      await delay()
      const t = mockStore.find((r) => r.id === Number(id))
      if (!t) throw new Error('Test case not found')
      return t
    }
    try { return await testCaseApi.getById(id) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  create: async (data) => {
    if (isMock) {
      await delay(500)
      const t = { ...data, id: nextId++, lastExecutionStatus: null, createdAt: new Date().toISOString().split('T')[0] }
      mockStore.push(t)
      return t
    }
    try { return await testCaseApi.create(data) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  update: async (id, data) => {
    if (isMock) {
      await delay(500)
      const idx = mockStore.findIndex((r) => r.id === Number(id))
      if (idx === -1) throw new Error('Test case not found')
      mockStore[idx] = { ...mockStore[idx], ...data }
      return mockStore[idx]
    }
    try { return await testCaseApi.update(id, data) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  delete: async (id) => {
    if (isMock) {
      await delay(400)
      const idx = mockStore.findIndex((r) => r.id === Number(id))
      if (idx === -1) throw new Error('Test case not found')
      mockStore.splice(idx, 1)
      return true
    }
    try { return await testCaseApi.delete(id) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },
}

export default testCaseService
