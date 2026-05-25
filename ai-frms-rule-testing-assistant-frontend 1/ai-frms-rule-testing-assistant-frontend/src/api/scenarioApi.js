import axiosInstance from './axiosConfig'

export const scenarioApi = {
  getAll: async (params = {}) => {
    const response = await axiosInstance.get('/test-scenarios', { params })
    return response.data
  },

  getById: async (id) => {
    const response = await axiosInstance.get(`/test-scenarios/${id}`)
    return response.data
  },

  create: async (data) => {
    const response = await axiosInstance.post('/test-scenarios', data)
    return response.data
  },

  update: async (id, data) => {
    const response = await axiosInstance.put(`/test-scenarios/${id}`, data)
    return response.data
  },

  delete: async (id) => {
    const response = await axiosInstance.delete(`/test-scenarios/${id}`)
    return response.data
  },

  getTestCases: async (scenarioId) => {
    const response = await axiosInstance.get(`/test-scenarios/${scenarioId}/test-cases`)
    return response.data
  },
}

export default scenarioApi
