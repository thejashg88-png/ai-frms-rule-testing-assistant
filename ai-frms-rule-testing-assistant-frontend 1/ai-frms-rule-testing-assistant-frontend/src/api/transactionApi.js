import axiosInstance from './axiosConfig'

export const transactionApi = {
  getAll: async (params = {}) => {
    const response = await axiosInstance.get('/transactions', { params })
    return response.data
  },

  getById: async (id) => {
    const response = await axiosInstance.get(`/transactions/${id}`)
    return response.data
  },

  create: async (data) => {
    const response = await axiosInstance.post('/transactions', data)
    return response.data
  },

  createBulk: async (data) => {
    const response = await axiosInstance.post('/transactions/bulk', data)
    return response.data
  },

  generateDummy: async (params = {}) => {
    const response = await axiosInstance.post('/transactions/generate-dummy', params)
    return response.data
  },

  generateHistory: async (payload) => {
    const response = await axiosInstance.post('/transactions/generate-history', payload)
    return response.data
  },

  delete: async (id) => {
    const response = await axiosInstance.delete(`/transactions/${id}`)
    return response.data
  },

  getStats: async () => {
    const response = await axiosInstance.get('/transactions/stats')
    return response.data
  },
}

export default transactionApi
