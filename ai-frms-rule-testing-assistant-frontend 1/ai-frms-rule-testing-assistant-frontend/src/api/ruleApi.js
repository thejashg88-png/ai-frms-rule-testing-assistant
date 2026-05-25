import axiosInstance from './axiosConfig'

export const ruleApi = {
  // Get all rules with optional filters
  getAllRules: async (params = {}) => {
    const response = await axiosInstance.get('/rules', { params })
    return response.data
  },

  // Get single rule by ID
  getRuleById: async (ruleId) => {
    const response = await axiosInstance.get(`/rules/${ruleId}`)
    return response.data
  },

  // Create new rule
  createRule: async (ruleData) => {
    const response = await axiosInstance.post('/rules', ruleData)
    return response.data
  },

  // Update existing rule
  updateRule: async (ruleId, ruleData) => {
    const response = await axiosInstance.put(`/rules/${ruleId}`, ruleData)
    return response.data
  },

  // Delete rule
  deleteRule: async (ruleId) => {
    const response = await axiosInstance.delete(`/rules/${ruleId}`)
    return response.data
  },

  // Get rule statistics
  getRuleStats: async () => {
    const response = await axiosInstance.get('/rules/stats')
    return response.data
  },
}

export default ruleApi