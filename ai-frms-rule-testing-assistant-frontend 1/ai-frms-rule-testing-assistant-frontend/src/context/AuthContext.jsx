import React, { createContext, useState, useEffect, useCallback } from 'react'
import { getToken, setToken, removeToken } from '../services/tokenService'
import { tokenService } from '../services/tokenService'
import authApi from '../api/authApi'
import { ROLES, canAccess } from '../utils/permissions'

export const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)

  // Restore session on mount
  useEffect(() => {
    const token = getToken()
    if (token) {
      const storedUser = tokenService.getUser()
      setUser(storedUser || { email: 'user@frms.com' })
      setIsAuthenticated(true)
    }
    setIsLoading(false)
  }, [])

  const role = user?.role || null

  useEffect(() => {
    console.log('[AUTH ROLE]', role)
  }, [role])

  const isAdmin  = role === ROLES.ADMIN
  const isTester = role === ROLES.TESTER
  const isViewer = role === ROLES.VIEWER

  const hasRole = useCallback((...roles) => roles.includes(role), [role])
  const can     = useCallback((permission) => canAccess(role, permission), [role])

  const login = async (email, password) => {
    try {
      setIsLoading(true)

      const useMock = import.meta.env.VITE_ENABLE_MOCK_LOGIN === 'true'

      if (useMock) {
        // Mock login defaults to ADMIN so all dev flows are accessible
        const mockToken = 'mock-jwt-' + Date.now()
        const userData = { id: 1, email, name: email.split('@')[0], role: ROLES.ADMIN }
        setToken(mockToken)
        tokenService.setUser(userData)
        setUser(userData)
        setIsAuthenticated(true)
        return { success: true }
      }

      // Real API call — backend wraps in ApiResponse<T>: { success, message, data: {...} }
      const response = await authApi.login(email, password)

      if (!response.success) {
        throw new Error(response.message || 'Login failed.')
      }

      const token = response.data?.token || response.token || response.accessToken

      if (!token) throw new Error('Login successful but token missing in response.')

      const userData = {
        id:       response.data?.userId,
        email:    response.data?.email    || email,
        username: response.data?.username,
        fullName: response.data?.fullName,
        role:     response.data?.role,
      }

      setToken(token)
      tokenService.setUser(userData)
      setUser(userData)
      setIsAuthenticated(true)
      return { success: true }
    } catch (error) {
      const message =
        error.response?.data?.message ||
        error.response?.data?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Invalid email or password'
      return { success: false, error: message }
    } finally {
      setIsLoading(false)
    }
  }

  const register = async ({ fullName, email, username, password }) => {
    try {
      const useMock = import.meta.env.VITE_ENABLE_MOCK_LOGIN === 'true'
      if (useMock) {
        await new Promise((r) => setTimeout(r, 600))
        return { success: true }
      }
      await authApi.register({ fullName, email, username, password })
      return { success: true }
    } catch (error) {
      const message =
        error.response?.data?.message ||
        error.response?.data?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Registration failed. Please check the details and try again.'
      return { success: false, error: message }
    }
  }

  const logout = () => {
    removeToken()
    tokenService.removeUser()
    setUser(null)
    setIsAuthenticated(false)
  }

  return (
    <AuthContext.Provider value={{
      user, role, isAdmin, isTester, isViewer,
      isAuthenticated, isLoading,
      hasRole, can,
      login, logout, register,
    }}>
      {children}
    </AuthContext.Provider>
  )
}
