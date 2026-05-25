import axiosInstance from './axiosConfig'

export const dashboardApi = {
  getSummary: async () => {
    const response = await axiosInstance.get('/dashboard/summary')
    return response.data
  },

  getRecentExecutions: async (limit = 10) => {
    const response = await axiosInstance.get('/dashboard/recent-executions', { params: { limit } })
    return response.data
  },

  getExecutionTrend: async (days = 7) => {
    const response = await axiosInstance.get('/dashboard/execution-trend', { params: { days } })
    return response.data
  },

  getRuleWiseStats: async () => {
    const response = await axiosInstance.get('/dashboard/rule-wise-stats')
    return response.data
  },
}

export default dashboardApi
