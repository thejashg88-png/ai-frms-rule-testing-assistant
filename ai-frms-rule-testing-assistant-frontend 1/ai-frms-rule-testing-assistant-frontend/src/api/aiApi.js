import axiosInstance from './axiosConfig'

export const aiApi = {
  generateTestCases: async (data) => {
    console.log('[aiApi.generateTestCases request payload]', data)
    const response = await axiosInstance.post('/ai/generate-test-cases', data)
    console.log('[aiApi.generateTestCases response.data]', response.data)
    return response.data
  },

  explainRule: async (data) => {
    console.log('[aiApi.explainRule request payload]', data)
    const response = await axiosInstance.post('/ai/explain-rule', data)
    console.log('[aiApi.explainRule axios full response]', response)
    console.log('[aiApi.explainRule response.data]', response.data)
    return response.data
  },

  analyzeFailure: async (data) => {
    console.log('[aiApi.analyzeFailure request payload]', data)
    const response = await axiosInstance.post('/ai/analyze-failure', data)
    console.log('[aiApi.analyzeFailure response.data]', response.data)
    return response.data
  },

  generateTransaction: async (data) => {
    const response = await axiosInstance.post('/ai/generate-transaction', data)
    return response.data
  },

  generateRule: async (requirement) => {
    const response = await axiosInstance.post('/ai/generate-rule', { requirement })
    return response.data
  },

  chat: async (payload) => {
    console.log('[AI Chat Request]', payload)
    const response = await axiosInstance.post('/ai/chat', payload)
    console.log('[AI Chat Response]', response.data)
    return response.data
  },
}

export default aiApi
