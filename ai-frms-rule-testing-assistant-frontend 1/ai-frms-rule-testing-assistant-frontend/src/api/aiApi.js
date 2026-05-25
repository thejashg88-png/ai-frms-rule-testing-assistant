import axiosInstance from './axiosConfig'

export const aiApi = {
  generateTestCases: async (data) => {
    const response = await axiosInstance.post('/ai/generate-test-cases', data)
    return response.data
  },

  explainRule: async (ruleId) => {
    const response = await axiosInstance.get(`/ai/explain-rule/${ruleId}`)
    return response.data
  },

  analyzeFailure: async (executionId) => {
    const response = await axiosInstance.get(`/ai/analyze-failure/${executionId}`)
    return response.data
  },

  generateTransaction: async (data) => {
    const response = await axiosInstance.post('/ai/generate-transaction', data)
    return response.data
  },

  chat: async (message, context = {}) => {
    const response = await axiosInstance.post('/ai/chat', { message, context })
    return response.data
  },
}

export default aiApi
