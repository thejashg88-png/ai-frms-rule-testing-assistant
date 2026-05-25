import axiosInstance from './axiosConfig'

export const reportApi = {
  getExecutionReport: async (params = {}) => {
    const response = await axiosInstance.get('/reports/executions', { params })
    return response.data
  },

  getRuleReport: async (params = {}) => {
    const response = await axiosInstance.get('/reports/rules', { params })
    return response.data
  },

  downloadReport: async (type, params = {}) => {
    const response = await axiosInstance.get(`/reports/download/${type}`, {
      params,
      responseType: 'blob',
    })
    return response.data
  },
}

export default reportApi
