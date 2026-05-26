import transactionApi from '../api/transactionApi'
import errorHandlerService from './errorHandlerService'

const isMock = import.meta.env.VITE_ENABLE_MOCK_DATA === 'true'
const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

const mapTransaction = (t) => ({
  ...t,
  id: t.transactionId ?? t.id,
  status: t.transactionStatus ?? t.status,
})

let nextId = 20
const mockStore = [
  {
    id: 1, transactionId: 'TXN-2025-001', cardNumber: '4532015112830366',
    amount: 1500.00, currency: 'USD', merchantId: 'M001', merchantName: 'Amazon Inc',
    merchantCategory: 'E-COMMERCE', transactionType: 'PURCHASE', channel: 'ONLINE',
    country: 'US', status: 'APPROVED', createdAt: '2025-01-15T10:30:00',
  },
  {
    id: 2, transactionId: 'TXN-2025-002', cardNumber: '5425233430109903',
    amount: 52000.00, currency: 'USD', merchantId: 'M002', merchantName: 'AutoDealer Pro',
    merchantCategory: 'AUTOMOTIVE', transactionType: 'PURCHASE', channel: 'POS',
    country: 'US', status: 'DECLINED', createdAt: '2025-01-15T11:00:00',
  },
  {
    id: 3, transactionId: 'TXN-2025-003', cardNumber: '4916338506082832',
    amount: 200.00, currency: 'USD', merchantId: 'M003', merchantName: 'Shell Gas Station',
    merchantCategory: 'FUEL', transactionType: 'PURCHASE', channel: 'ATM',
    country: 'US', status: 'APPROVED', createdAt: '2025-01-14T09:15:00',
  },
  {
    id: 4, transactionId: 'TXN-2025-004', cardNumber: '4532015112830366',
    amount: 350.00, currency: 'EUR', merchantId: 'M004', merchantName: 'Paris Boutique',
    merchantCategory: 'RETAIL', transactionType: 'PURCHASE', channel: 'POS',
    country: 'FR', status: 'DECLINED', createdAt: '2025-01-14T14:20:00',
  },
  {
    id: 5, transactionId: 'TXN-2025-005', cardNumber: '5425233430109903',
    amount: 75.50, currency: 'USD', merchantId: 'M005', merchantName: 'Uber Eats',
    merchantCategory: 'FOOD', transactionType: 'PURCHASE', channel: 'MOBILE',
    country: 'US', status: 'APPROVED', createdAt: '2025-01-13T19:45:00',
  },
  {
    id: 6, transactionId: 'TXN-2025-006', cardNumber: '4916338506082832',
    amount: 5000.00, currency: 'USD', merchantId: 'M006', merchantName: 'Wire Transfer',
    merchantCategory: 'TRANSFER', transactionType: 'TRANSFER', channel: 'ONLINE',
    country: 'US', status: 'PENDING', createdAt: '2025-01-13T16:00:00',
  },
  {
    id: 7, transactionId: 'TXN-2025-007', cardNumber: '4532015112830366',
    amount: 120.00, currency: 'USD', merchantId: 'M001', merchantName: 'Amazon Inc',
    merchantCategory: 'E-COMMERCE', transactionType: 'REFUND', channel: 'ONLINE',
    country: 'US', status: 'APPROVED', createdAt: '2025-01-12T10:00:00',
  },
  {
    id: 8, transactionId: 'TXN-2025-008', cardNumber: '5425233430109903',
    amount: 300.00, currency: 'USD', merchantId: 'M007', merchantName: 'ATM Withdrawal',
    merchantCategory: 'CASH', transactionType: 'WITHDRAWAL', channel: 'ATM',
    country: 'US', status: 'APPROVED', createdAt: '2025-01-12T08:30:00',
  },
]

const applyFilters = (data, params) => {
  let result = [...data]
  if (params.status)          result = result.filter((t) => t.status === params.status)
  if (params.transactionType) result = result.filter((t) => t.transactionType === params.transactionType)
  if (params.channel)         result = result.filter((t) => t.channel === params.channel)
  if (params.search) {
    const q = params.search.toLowerCase()
    result = result.filter(
      (t) =>
        t.transactionId.toLowerCase().includes(q) ||
        t.merchantName.toLowerCase().includes(q) ||
        t.cardNumber.slice(-4).includes(q)
    )
  }
  return result
}

export const transactionService = {
  getAll: async (params = {}) => {
    if (isMock) { await delay(); return applyFilters(mockStore, params) }
    try {
      const resp = await transactionApi.getAll(params)
      const items = resp?.data?.content ?? (Array.isArray(resp?.data) ? resp.data : [])
      return items.map(mapTransaction)
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  getById: async (id) => {
    if (isMock) {
      await delay()
      const t = mockStore.find((r) => r.id === Number(id))
      if (!t) throw new Error('Transaction not found')
      return t
    }
    try {
      const resp = await transactionApi.getById(id)
      return mapTransaction(resp?.data ?? resp)
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  create: async (data) => {
    if (isMock) {
      await delay(500)
      const t = { ...data, id: nextId++, createdAt: new Date().toISOString() }
      mockStore.push(t)
      return t
    }
    try {
      const resp = await transactionApi.create(data)
      return mapTransaction(resp?.data ?? resp)
    }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },

  delete: async (id) => {
    if (isMock) {
      await delay(400)
      const idx = mockStore.findIndex((r) => r.id === Number(id))
      if (idx === -1) throw new Error('Transaction not found')
      mockStore.splice(idx, 1)
      return true
    }
    try { return await transactionApi.delete(id) }
    catch (err) { throw new Error(errorHandlerService.getErrorMessage(err)) }
  },
}

export default transactionService
