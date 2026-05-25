export const localStorageService = {
  // Get item with optional JSON parsing
  getItem: (key, parse = false) => {
    const value = localStorage.getItem(key)
    return parse && value ? JSON.parse(value) : value
  },

  // Set item with optional JSON stringification
  setItem: (key, value, stringify = false) => {
    const data = stringify ? JSON.stringify(value) : value
    localStorage.setItem(key, data)
  },

  // Remove item
  removeItem: (key) => {
    localStorage.removeItem(key)
  },

  // Clear all localStorage
  clear: () => {
    localStorage.clear()
  },

  // Check if item exists
  has: (key) => {
    return localStorage.getItem(key) !== null
  },

  // Get all keys
  getAllKeys: () => {
    return Object.keys(localStorage)
  },
}

export default localStorageService