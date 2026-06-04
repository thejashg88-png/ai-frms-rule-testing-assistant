import axiosInstance from './axiosConfig'

export const auditApi = {
  getAll: async (params = {}) => {
    const response = await axiosInstance.get('/audit-logs', { params })
    return response.data
  },
}

export default auditApi
