import axiosInstance from './axiosConfig'

// All functions return response.data — the backend wraps everything in
// ApiResponse<T> { success, message, data, timestamp }; ruleService unwraps further.
export const ruleApi = {
  // Supports page/size params; ruleService requests size=500 to load all at once.
  getAllRules: async (params = {}) => {
    const response = await axiosInstance.get('/rules', { params })
    return response.data
  },

  getRuleById: async (ruleId) => {
    const response = await axiosInstance.get(`/rules/${ruleId}`)
    return response.data
  },

  // Payload is shaped by ruleService.toApiPayload — field names must match backend RuleDTO.
  createRule: async (ruleData) => {
    const response = await axiosInstance.post('/rules', ruleData)
    return response.data
  },

  updateRule: async (ruleId, ruleData) => {
    const response = await axiosInstance.put(`/rules/${ruleId}`, ruleData)
    return response.data
  },

  deleteRule: async (ruleId) => {
    const response = await axiosInstance.delete(`/rules/${ruleId}`)
    return response.data
  },

  getRuleStats: async () => {
    const response = await axiosInstance.get('/rules/stats')
    return response.data
  },
}

export default ruleApi