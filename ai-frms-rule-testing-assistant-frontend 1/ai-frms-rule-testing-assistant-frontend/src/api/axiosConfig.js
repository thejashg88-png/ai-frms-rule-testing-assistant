import axios from 'axios'
import { getToken, tokenService } from '../services/tokenService'

// Base URL is set via VITE_API_BASE_URL in .env; falls back to local Spring Boot default.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

// Shared axios instance used by all *Api.js modules.
// All API functions return response.data so services can normalize the
// backend ApiResponse<T> wrapper consistently without touching the raw axios response.
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Attach JWT and actor username before every request.
// Token and user are stored by tokenService after login.
axiosInstance.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    const user = tokenService.getUser()
    const username = user?.username || user?.fullName || user?.email || null
    console.log('[API Request Actor]', username)
    if (username) {
      config.headers['X-Actor-Username'] = username
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Global response error handler.
// 401 → redirect to login; 403 → attach a clear permission-denied message.
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/login'
    }
    if (error.response?.status === 403) {
      error.message =
        error.response?.data?.message ||
        'You do not have permission to perform this action.'
    }
    return Promise.reject(error)
  }
)

export default axiosInstance