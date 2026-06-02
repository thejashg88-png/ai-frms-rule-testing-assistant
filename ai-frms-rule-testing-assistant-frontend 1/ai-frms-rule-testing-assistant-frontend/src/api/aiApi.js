import axiosInstance from './axiosConfig'

// All AI endpoints proxy to the Spring Boot backend, which forwards to the FastAPI AI service.
// All functions return response.data; aiService normalizes the nested JSON response shapes.
export const aiApi = {
  // Payload: { ruleId, ruleName, ruleType, requirement }
  generateTestCases: async (data) => {
    console.log('[aiApi.generateTestCases request payload]', data)
    const response = await axiosInstance.post('/ai/generate-test-cases', data)
    console.log('[aiApi.generateTestCases response.data]', response.data)
    return response.data
  },

  // Payload: rule fields (ruleId, ruleType, thresholds, description, etc.)
  explainRule: async (data) => {
    console.log('[aiApi.explainRule request payload]', data)
    const response = await axiosInstance.post('/ai/explain-rule', data)
    console.log('[aiApi.explainRule axios full response]', response)
    console.log('[aiApi.explainRule response.data]', response.data)
    return response.data
  },

  // Payload: { executionId, testCaseName, ruleType, expectedResult, actualResult, inputData, executionLogs }
  analyzeFailure: async (data) => {
    console.log('[aiApi.analyzeFailure request payload]', data)
    const response = await axiosInstance.post('/ai/analyze-failure', data)
    console.log('[aiApi.analyzeFailure response.data]', response.data)
    return response.data
  },

  // Payload: hint fields (maxAmount, channel, transactionType)
  generateTransaction: async (data) => {
    const response = await axiosInstance.post('/ai/generate-transaction', data)
    return response.data
  },

  // Payload: { requirement } — plain-English rule description
  generateRule: async (requirement) => {
    const response = await axiosInstance.post('/ai/generate-rule', { requirement })
    return response.data
  },

  // Payload: { message, context } — AiChatRequest DTO on the Java backend.
  // Backend stores FastAPI JSON response verbatim in AiChatResponse.reply;
  // aiService.normalizeAiChatResponse handles JSON-string unwrapping.
  chat: async (payload) => {
    console.log('[AI Chat Request]', payload)
    const response = await axiosInstance.post('/ai/chat', payload)
    console.log('[AI Chat Response]', response.data)
    return response.data
  },
}

export default aiApi
