import axios from 'axios'
import { getToken } from '../services/tokenService'

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

// Attach JWT from localStorage before every request.
// Token is stored by tokenService after login; absent when the user is logged out.
axiosInstance.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Global 401 handler — redirect to /login when the token has expired or is invalid.
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default axiosInstance