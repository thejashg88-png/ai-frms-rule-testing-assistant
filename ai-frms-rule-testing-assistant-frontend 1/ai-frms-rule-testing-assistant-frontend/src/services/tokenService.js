const TOKEN_KEY = 'auth_token'
const USER_KEY = 'user_data'

export const tokenService = {
  // Get token from localStorage
  getToken: () => {
    return localStorage.getItem(TOKEN_KEY)
  },

  // Set token in localStorage
  setToken: (token) => {
    localStorage.setItem(TOKEN_KEY, token)
  },

  // Remove token from localStorage
  removeToken: () => {
    localStorage.removeItem(TOKEN_KEY)
  },

  // Check if token exists
  hasToken: () => {
    return !!localStorage.getItem(TOKEN_KEY)
  },

  // Get user data
  getUser: () => {
    const user = localStorage.getItem(USER_KEY)
    return user ? JSON.parse(user) : null
  },

  // Set user data
  setUser: (user) => {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },

  // Remove user data
  removeUser: () => {
    localStorage.removeItem(USER_KEY)
  },

  // Clear all auth data
  clearAuth: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  },
}

// Export individual functions for convenience
export const getToken = () => tokenService.getToken()
export const setToken = (token) => tokenService.setToken(token)
export const removeToken = () => tokenService.removeToken()
export const hasToken = () => tokenService.hasToken()