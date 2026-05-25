import scenarioApi from '../api/scenarioApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

let nextId = 10
const mockStore = [
  {
    id: 1, name: 'High Value Transaction Tests', status: 'ACTIVE',
    description: 'Validates all rules triggered by transactions above $10,000',
    scenarioType: 'BULK', ruleIds: [1, 2], testCaseCount: 5, createdAt: '2025-01-15',
  },
  {
    id: 2, name: 'Card Velocity Scenario', status: 'ACTIVE',
    description: 'Tests rapid consecutive card transactions within a rolling window',
    scenarioType: 'BULK', ruleIds: [3], testCaseCount: 4, createdAt: '2025-01-14',
  },
  {
    id: 3, name: 'Geo Mismatch Detection', status: 'ACTIVE',
    description: 'Scenarios for cards used in countries inconsistent with issuer location',
    scenarioType: 'SINGLE', ruleIds: [4], testCaseCount: 3, createdAt: '2025-01-13',
  },
  {
    id: 4, name: 'Duplicate Transaction Prevention', status: 'INACTIVE',
    description: 'Ensures duplicate transactions are detected and blocked within a session',
    scenarioType: 'SINGLE', ruleIds: [5], testCaseCount: 2, createdAt: '2025-01-12',
  },
  {
    id: 5, name: 'Foreign Card Compliance', status: 'ACTIVE',
    description: 'Extra verification tests for foreign-issued cards at domestic merchants',
    scenarioType: 'BULK', ruleIds: [6], testCaseCount: 3, createdAt: '2025-01-11',
  },
]

const applyFilters = (data, params) => {
  let result = [...data]
  if (params.status)       result = result.filter((s) => s.status === params.status)
  if (params.scenarioType) result = result.filter((s) => s.scenarioType === params.scenarioType)
  if (params.search) {
    const q = params.search.toLowerCase()
    result = result.filter(
      (s) => s.name.toLowerCase().includes(q) || (s.description || '').toLowerCase().includes(q)
    )
  }
  return result
}

export const scenarioService = {
  getAll: async (params = {}) => {
    if (isMock) { await delay(); return applyFilters(mockStore, params) }
    try { return await scenarioApi.getAll(params) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  getById: async (id) => {
    if (isMock) {
      await delay()
      const s = mockStore.find((r) => r.id === Number(id))
      if (!s) throw new Error('Scenario not found')
      return s
    }
    try { return await scenarioApi.getById(id) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  create: async (data) => {
    if (isMock) {
      await delay(500)
      const s = { ...data, id: nextId++, testCaseCount: 0, createdAt: new Date().toISOString().split('T')[0] }
      mockStore.push(s)
      return s
    }
    try { return await scenarioApi.create(data) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  update: async (id, data) => {
    if (isMock) {
      await delay(500)
      const idx = mockStore.findIndex((r) => r.id === Number(id))
      if (idx === -1) throw new Error('Scenario not found')
      mockStore[idx] = { ...mockStore[idx], ...data }
      return mockStore[idx]
    }
    try { return await scenarioApi.update(id, data) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  delete: async (id) => {
    if (isMock) {
      await delay(400)
      const idx = mockStore.findIndex((r) => r.id === Number(id))
      if (idx === -1) throw new Error('Scenario not found')
      mockStore.splice(idx, 1)
      return true
    }
    try { return await scenarioApi.delete(id) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },
}

export default scenarioService
