import ruleApi from '../api/ruleApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

// Normalizes backend field names (ruleId, ruleName, ruleDescription) to frontend-standard names.
// Multiple field names are checked because the backend may return either naming convention.
const mapRule = (r) => ({
  ...r,
  id:          r.ruleId       ?? r.id,
  name:        r.ruleName     ?? r.name,
  description: r.ruleDescription ?? r.description,
  frequency:   r.frequencyHours  ?? r.frequency,
})

const numOrNull = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

// Converts frontend form shape → backend RuleDTO field names.
// txnAmount is zero-padded to 12 digits because the backend stores it as a fixed-length string
// (e.g. "00000150000") and uses it for threshold comparisons in the rule engine.
// Fields not applicable to the selected ruleType are sent as null — never omitted —
// so the backend can explicitly clear them on update.
const toApiPayload = (data) => {
  const txnAmountRaw = numOrNull(data.txnAmount ?? data.txnAmountRaw)
  const payload = {
    ruleName:            (data.ruleName     ?? data.name        ?? '').trim(),
    ruleDescription:     (data.ruleDescription ?? data.description ?? '').trim(),
    ruleType:            data.ruleType,
    action:              data.action,
    status:              data.status ?? 'ACTIVE',
    txnCount:            numOrNull(data.txnCount),
    txnAmount:           txnAmountRaw != null ? String(txnAmountRaw).padStart(12, '0') : null,
    frequency:           numOrNull(data.frequency),
    maxAmount:           numOrNull(data.maxAmount),
    percentageThreshold: numOrNull(data.percentageThreshold),
  }
  console.log('[Create Rule Payload]', payload)
  return payload
}

let nextId = 10
const mockStore = [
  {
    id: 1, name: 'Credit Limit Check',
    description: 'Validates customer credit limit against transaction amount',
    ruleType: 'CREDIT', action: 'REJECT', status: 'ACTIVE',
    txnCount: 5, maxAmount: 10000, txnAmount: 500, frequency: 3, percentageThreshold: 80,
    createdAt: '2025-01-15',
  },
  {
    id: 2, name: 'High Value TX Alert',
    description: 'Flags transactions above threshold for compliance review',
    ruleType: 'AMOUNT', action: 'MONITOR', status: 'ACTIVE',
    txnCount: 1, maxAmount: 50000, txnAmount: 5000, frequency: null, percentageThreshold: null,
    createdAt: '2025-01-14',
  },
  {
    id: 3, name: 'Card Velocity Rule',
    description: 'Detects high-frequency card usage within a rolling time window',
    ruleType: 'VELOCITY', action: 'REJECT', status: 'ACTIVE',
    txnCount: 10, maxAmount: 2000, txnAmount: 200, frequency: 10, percentageThreshold: null,
    createdAt: '2025-01-13',
  },
  {
    id: 4, name: 'Geo Mismatch Detect',
    description: 'Identifies geographic location inconsistencies between issuer and merchant',
    ruleType: 'GEO', action: 'MONITOR', status: 'ACTIVE',
    txnCount: null, maxAmount: null, txnAmount: null, frequency: null, percentageThreshold: 90,
    createdAt: '2025-01-12',
  },
  {
    id: 5, name: 'Duplicate TXN Check',
    description: 'Prevents duplicate transaction processing within same session',
    ruleType: 'FRAUD', action: 'REJECT', status: 'ACTIVE',
    txnCount: 2, maxAmount: null, txnAmount: null, frequency: 5, percentageThreshold: null,
    createdAt: '2025-01-11',
  },
  {
    id: 6, name: 'Foreign Card Rule',
    description: 'Applies additional verification checks for foreign-issued cards',
    ruleType: 'CARD', action: 'MONITOR', status: 'INACTIVE',
    txnCount: null, maxAmount: 3000, txnAmount: null, frequency: null, percentageThreshold: null,
    createdAt: '2025-01-10',
  },
  {
    id: 7, name: 'Daily Frequency Limit',
    description: 'Limits total transactions per account per calendar day',
    ruleType: 'FREQUENCY', action: 'REJECT', status: 'ACTIVE',
    txnCount: 20, maxAmount: null, txnAmount: null, frequency: 20, percentageThreshold: null,
    createdAt: '2025-01-09',
  },
]

const applyFilters = (data, params) => {
  let result = [...data]
  if (params.status)   result = result.filter((r) => r.status === params.status)
  if (params.ruleType) result = result.filter((r) => r.ruleType === params.ruleType)
  if (params.search) {
    const q = params.search.toLowerCase()
    result = result.filter(
      (r) => r.name.toLowerCase().includes(q) || (r.description || '').toLowerCase().includes(q)
    )
  }
  return result
}

export const ruleService = {
  // Returns ALL rules unfiltered; pages do client-side filtering + pagination.
  getAll: async () => {
    if (isMock) {
      await delay()
      return mockStore.map(mapRule)
    }
    try {
      const resp = await ruleApi.getAllRules({ page: 0, size: 500 })
      console.log('[Rules API Response]', resp)
      // Normalise all backend response shapes
      const raw =
        resp?.data?.content  ??
        resp?.data?.rules    ??
        (Array.isArray(resp?.data) ? resp.data : null) ??
        resp?.content        ??
        resp?.rules          ??
        (Array.isArray(resp) ? resp : [])
      const normalized = (Array.isArray(raw) ? raw : []).map(mapRule)
      console.log('[Rules Normalized]', normalized)
      return normalized
    } catch (err) {
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },

  getById: async (id) => {
    if (isMock) {
      await delay()
      const rule = mockStore.find((r) => r.id === Number(id))
      if (!rule) throw new Error('Rule not found')
      return rule
    }
    try {
      const resp = await ruleApi.getRuleById(id)
      return mapRule(resp?.data ?? resp)
    } catch (err) {
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },

  create: async (data) => {
    if (isMock) {
      await delay(500)
      const rule = {
        ...data,
        id: nextId++,
        createdAt: new Date().toISOString().split('T')[0],
      }
      mockStore.push(rule)
      return rule
    }
    try {
      const payload = toApiPayload(data)
      const resp = await ruleApi.createRule(payload)
      return mapRule(resp?.data ?? resp)
    } catch (err) {
      console.error('[Create Rule Error Response]', err.response?.data)
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },

  update: async (id, data) => {
    if (isMock) {
      await delay(500)
      const idx = mockStore.findIndex((r) => r.id === Number(id))
      if (idx === -1) throw new Error('Rule not found')
      mockStore[idx] = { ...mockStore[idx], ...data }
      return mockStore[idx]
    }
    try {
      const payload = toApiPayload(data)
      const resp = await ruleApi.updateRule(id, payload)
      return mapRule(resp?.data ?? resp)
    } catch (err) {
      console.error('[Update Rule Error Response]', err.response?.data)
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },

  delete: async (id) => {
    if (isMock) {
      await delay(400)
      const idx = mockStore.findIndex((r) => r.id === Number(id))
      if (idx === -1) throw new Error('Rule not found')
      mockStore.splice(idx, 1)
      return true
    }
    try {
      return await ruleApi.deleteRule(id)
    } catch (err) {
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },
}

export default ruleService
