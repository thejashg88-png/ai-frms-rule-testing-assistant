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

  runTestCase: async (testCaseId, data = {}) => {
    const response = await axiosInstance.post(`/executions/run-testcase/${testCaseId}`, data)
    return response.data
  },

  runScenario: async (scenarioId, data = {}) => {
    const response = await axiosInstance.post(`/executions/run-scenario/${scenarioId}`, data)
    return response.data
  },

  getStats: async () => {
    const response = await axiosInstance.get('/executions/stats')
    return response.data
  },
}

export default executionApi
