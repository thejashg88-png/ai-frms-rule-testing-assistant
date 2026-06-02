import axiosInstance from './axiosConfig'

// All functions return response.data; executionService normalizes the body further.
export const executionApi = {
  getAll: async (params = {}) => {
    const response = await axiosInstance.get('/executions', { params })
    return response.data
  },

  getById: async (id) => {
    const response = await axiosInstance.get(`/executions/${id}`)
    return response.data
  },

  // Runs a single test case. Payload: { testCaseId, executedBy }.
  // Response body contains executionStatus + results[] array.
  runTestCase: async (payload) => {
    const response = await axiosInstance.post('/executions/test-case', payload)
    return response.data
  },

  // Runs all ACTIVE test cases inside a scenario in one call.
  // Payload: { scenarioId, executedBy }.
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
