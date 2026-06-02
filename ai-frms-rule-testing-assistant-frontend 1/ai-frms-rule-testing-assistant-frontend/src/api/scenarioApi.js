import axiosInstance from './axiosConfig'

// All functions return response.data; scenarioService normalizes the body.
export const scenarioApi = {
  getAll: async (params = {}) => {
    const response = await axiosInstance.get('/scenarios', { params })
    return response.data
  },

  getById: async (id) => {
    const response = await axiosInstance.get(`/scenarios/${id}`)
    return response.data
  },

  create: async (data) => {
    const response = await axiosInstance.post('/scenarios', data)
    return response.data
  },

  update: async (id, data) => {
    const response = await axiosInstance.put(`/scenarios/${id}`, data)
    return response.data
  },

  delete: async (id) => {
    const response = await axiosInstance.delete(`/scenarios/${id}`)
    return response.data
  },

  // Returns the test cases associated with a specific scenario.
  getTestCases: async (scenarioId) => {
    const response = await axiosInstance.get(`/scenarios/${scenarioId}/testcases`)
    return response.data
  },
}

export default scenarioApi
