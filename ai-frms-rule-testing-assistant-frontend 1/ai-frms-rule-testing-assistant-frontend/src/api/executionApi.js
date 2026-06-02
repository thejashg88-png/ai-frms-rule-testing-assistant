import axiosInstance from './axiosConfig'

export const executionApi = {
  getAll: async (params = {}) => {
    const response = await axiosInstance.get('/executions', { params })
    return response.data
  },

  getById: async (id) => {
    const response = await axiosInstance.get(`/executions/${id}`)
    return response.data
  },

  runTestCase: async (payload) => {
    const response = await axiosInstance.post('/executions/test-case', payload)
    return response.data
  },

  runScenario: async (payload) => {
    const response = await axiosInstance.post('/executions/scenario', payload)
    return response.data
  },

  getStats: async () => {
    const response = await axiosInstance.get('/executions/stats')
    return response.data
  },
}

export default executionApi
