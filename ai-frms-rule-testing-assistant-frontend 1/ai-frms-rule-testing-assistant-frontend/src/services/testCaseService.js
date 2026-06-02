import testCaseApi from '../api/testCaseApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

const mapTestCase = (t) => ({
  ...t,
  id:          t.testCaseId          ?? t.id,
  name:        t.testCaseName        ?? t.name,
  description: t.testCaseDescription ?? t.description,
})

// Extracts the field-level error messages from a Spring Validation error response.
// Backend returns: { message: "Validation failed", errors: ["field: msg", ...], ... }
const extractValidationErrors = (err) => {
  const data = err?.response?.data
  console.error('[Create Test Case Error Response]', data)

  const errs = data?.errors
  if (Array.isArray(errs) && errs.length > 0) {
    console.error('[Create Test Case Validation Errors]', errs)
    return errs
      .map((e) =>
        typeof e === 'string' ? e
        : e.defaultMessage   ? `${e.field ?? ''}: ${e.defaultMessage}`
        : e.message          ? e.message
        : JSON.stringify(e)
      )
      .join('; ')
  }
  return null
}

// Converts the flat TestCaseForm state to the nested TestCaseDTO the backend expects.
// expectedResult MUST remain an object (ExpectedResult DTO) — never a plain string.
// Sending it as a string breaks backend deserialization.
// inputData defaults currency to 'INR' because the rule engine requires a non-null currency.
const toApiPayload = (data) => {
  const payload = {
    testCaseName: (data.testCaseName ?? data.name ?? '').trim(),
    description:  (data.description ?? '').trim(),
    scenarioId:   data.scenarioId ? Number(data.scenarioId) : null,
    scenarioName: (data.scenarioName ?? '').trim(),
    ruleId:       data.ruleId ? Number(data.ruleId) : null,
    ruleName:     (data.ruleName ?? '').trim(),
    status:       data.status ?? 'ACTIVE',
    inputData: {
      cardNumber:      (data.inputData?.cardNumber      ?? '').trim(),
      amount:          data.inputData?.amount,
      merchantId:      (data.inputData?.merchantId      ?? '').trim(),
      transactionType: data.inputData?.transactionType  ?? 'PURCHASE',
      channel:         data.inputData?.channel           ?? 'ONLINE',
      countryCode:     (data.inputData?.countryCode ?? data.inputData?.country ?? '').trim().toUpperCase(),
      currency:        data.inputData?.currency          || 'INR',
    },
    // expectedResult is an object matching backend ExpectedResult DTO —
    // do not flatten these fields to the top level.
    expectedResult: {
      expectedOutcome:   data.expectedResult    ?? 'PASS',
      expectedAction:    data.expectedAction     ?? 'MONITOR',
      expectedRiskLevel: data.expectedRiskLevel  ?? 'MEDIUM',
      expectedRuleType:  (data.ruleType ?? data.expectedRuleType ?? '').trim(),
      remarks:           (data.remarks ?? '').trim(),
    },
  }
  console.log('[Create Test Case Payload]', payload)
  return payload
}

let nextId = 20
const mockStore = [
  {
    id: 1, name: 'Credit limit exceeded — should REJECT',
    description: 'Amount exceeds credit limit by 50%',
    scenarioId: 1, scenarioName: 'High Value Transaction Tests',
    ruleId: 1, ruleName: 'Credit Limit Check',
    inputData: { amount: 15000, cardNumber: '4532015112830366', merchantId: 'M001', transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US', currency: 'USD' },
    expectedResult: 'FAIL', expectedAction: 'REJECT', status: 'ACTIVE',
    lastExecutionStatus: 'PASSED', createdAt: '2025-01-15',
  },
  {
    id: 2, name: 'Amount within limit — should ACCEPT',
    description: 'Transaction within normal credit range',
    scenarioId: 1, scenarioName: 'High Value Transaction Tests',
    ruleId: 1, ruleName: 'Credit Limit Check',
    inputData: { amount: 500, cardNumber: '5425233430109903', merchantId: 'M002', transactionType: 'PURCHASE', channel: 'POS', country: 'US', currency: 'USD' },
    expectedResult: 'PASS', expectedAction: 'ACCEPT', status: 'ACTIVE',
    lastExecutionStatus: 'PASSED', createdAt: '2025-01-15',
  },
  {
    id: 3, name: 'High value alert — should MONITOR',
    description: 'Transaction triggers high-value monitoring alert',
    scenarioId: 1, scenarioName: 'High Value Transaction Tests',
    ruleId: 2, ruleName: 'High Value TX Alert',
    inputData: { amount: 55000, cardNumber: '4916338506082832', merchantId: 'M003', transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US', currency: 'USD' },
    expectedResult: 'PASS', expectedAction: 'MONITOR', status: 'ACTIVE',
    lastExecutionStatus: 'FAILED', createdAt: '2025-01-14',
  },
  {
    id: 4, name: 'Velocity breach — 12 txns in 1 hour',
    description: 'Card used 12 times in rolling window, exceeds velocity rule',
    scenarioId: 2, scenarioName: 'Card Velocity Scenario',
    ruleId: 3, ruleName: 'Card Velocity Rule',
    inputData: { amount: 200, cardNumber: '4532015112830366', merchantId: 'M005', transactionType: 'PURCHASE', channel: 'POS', country: 'US', currency: 'USD' },
    expectedResult: 'FAIL', expectedAction: 'REJECT', status: 'ACTIVE',
    lastExecutionStatus: 'PASSED', createdAt: '2025-01-14',
  },
  {
    id: 5, name: 'Geo mismatch — card issued in US, used in EU',
    description: 'Card issued in US being used in France without prior travel notice',
    scenarioId: 3, scenarioName: 'Geo Mismatch Detection',
    ruleId: 4, ruleName: 'Geo Mismatch Detect',
    inputData: { amount: 350, cardNumber: '5425233430109903', merchantId: 'M004', transactionType: 'PURCHASE', channel: 'POS', country: 'FR', currency: 'EUR' },
    expectedResult: 'PASS', expectedAction: 'MONITOR', status: 'ACTIVE',
    lastExecutionStatus: 'PASSED', createdAt: '2025-01-13',
  },
  {
    id: 6, name: 'Duplicate transaction within same session',
    description: 'Same card, merchant and amount submitted twice in 60 seconds',
    scenarioId: 4, scenarioName: 'Duplicate Transaction Prevention',
    ruleId: 5, ruleName: 'Duplicate TXN Check',
    inputData: { amount: 120, cardNumber: '4916338506082832', merchantId: 'M001', transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US', currency: 'USD' },
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
  // Returns ALL test cases unfiltered; pages do client-side filtering + pagination.
  getAll: async () => {
    if (isMock) { await delay(); return mockStore.map(mapTestCase) }
    try {
      const resp = await testCaseApi.getAll({ page: 0, size: 500 })
      console.log('[TestCases API Response]', resp)
      const raw =
        resp?.data?.content    ??
        resp?.data?.testCases  ??
        (Array.isArray(resp?.data) ? resp.data : null) ??
        resp?.content          ??
        resp?.testCases        ??
        (Array.isArray(resp) ? resp : [])
      const normalized = (Array.isArray(raw) ? raw : []).map(mapTestCase)
      console.log('[TestCases Normalized]', normalized)
      return normalized
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  getById: async (id) => {
    if (isMock) {
      await delay()
      const t = mockStore.find((r) => r.id === Number(id))
      if (!t) throw new Error('Test case not found')
      return t
    }
    try {
      const resp = await testCaseApi.getById(id)
      return mapTestCase(resp?.data ?? resp)
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  create: async (data) => {
    if (isMock) {
      await delay(500)
      const t = { ...data, id: nextId++, lastExecutionStatus: null, createdAt: new Date().toISOString().split('T')[0] }
      mockStore.push(t)
      return t
    }
    try {
      const payload = toApiPayload(data)
      const resp = await testCaseApi.create(payload)
      return mapTestCase(resp?.data ?? resp)
    }
    catch (err) {
      const detail = extractValidationErrors(err)
      if (detail) throw new Error(`Validation failed: ${detail}`)
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },

  update: async (id, data) => {
    if (isMock) {
      await delay(500)
      const idx = mockStore.findIndex((r) => r.id === Number(id))
      if (idx === -1) throw new Error('Test case not found')
      mockStore[idx] = { ...mockStore[idx], ...data }
      return mockStore[idx]
    }
    try {
      const resp = await testCaseApi.update(id, data)
      return mapTestCase(resp?.data ?? resp)
    }
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
