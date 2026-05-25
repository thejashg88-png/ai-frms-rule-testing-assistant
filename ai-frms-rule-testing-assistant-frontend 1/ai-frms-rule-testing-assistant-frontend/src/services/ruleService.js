import ruleApi from '../api/ruleApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

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
  getAll: async (params = {}) => {
    if (isMock) {
      await delay()
      return applyFilters(mockStore, params)
    }
    try {
      return await ruleApi.getAllRules(params)
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
      return await ruleApi.getRuleById(id)
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
      return await ruleApi.createRule(data)
    } catch (err) {
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
      return await ruleApi.updateRule(id, data)
    } catch (err) {
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
