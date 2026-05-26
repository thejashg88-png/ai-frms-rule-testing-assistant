import axiosInstance from './axiosConfig'

export const testCaseApi = {
  getAll: async (params = {}) => {
    const response = await axiosInstance.get('/testcases', { params })
    return response.data
  },

  getById: async (id) => {
    const response = await axiosInstance.get(`/testcases/${id}`)
    return response.data
  },

  create: async (data) => {
    const response = await axiosInstance.post('/testcases', data)
    return response.data
  },

  update: async (id, data) => {
    const response = await axiosInstance.put(`/testcases/${id}`, data)
    return response.data
  },

  delete: async (id) => {
    const response = await axiosInstance.delete(`/testcases/${id}`)
    return response.data
  },
}

export default testCaseApi
