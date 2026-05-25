import axiosInstance from './axiosConfig'

export const authApi = {
  login: async (email, password) => {
    const response = await axiosInstance.post('/auth/login', { email, password })
    return response.data
  },

  register: async ({ fullName, email, username, password }) => {
    const response = await axiosInstance.post('/auth/register', { fullName, email, username, password })
    return response.data
  },

  logout: async () => {
    const response = await axiosInstance.post('/auth/logout')
    return response.data
  },

  verifyToken: async () => {
    const response = await axiosInstance.get('/auth/verify')
    return response.data
  },

  refreshToken: async () => {
    const response = await axiosInstance.post('/auth/refresh')
    return response.data
  },

  getCurrentUser: async () => {
    const response = await axiosInstance.get('/auth/me')
    return response.data
  },
}

export default authApi