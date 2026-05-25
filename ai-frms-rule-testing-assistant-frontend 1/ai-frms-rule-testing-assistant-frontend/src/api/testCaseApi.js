import axiosInstance from './axiosConfig'

export const testCaseApi = {
  getAll: async (params = {}) => {
    const response = await axiosInstance.get('/test-cases', { params })
    return response.data
  },

  getById: async (id) => {
    const response = await axiosInstance.get(`/test-cases/${id}`)
    return response.data
  },

  create: async (data) => {
    const response = await axiosInstance.post('/test-cases', data)
    return response.data
  },

  update: async (id, data) => {
    const response = await axiosInstance.put(`/test-cases/${id}`, data)
    return response.data
  },

  delete: async (id) => {
    const response = await axiosInstance.delete(`/test-cases/${id}`)
    return response.data
  },
}

export default testCaseApi
