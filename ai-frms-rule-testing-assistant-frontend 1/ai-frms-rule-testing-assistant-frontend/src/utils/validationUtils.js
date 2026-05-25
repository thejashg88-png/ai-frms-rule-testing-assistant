export const validationUtils = {
  // Validate email
  isValidEmail: (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  },

  // Validate password (min 8 chars, 1 uppercase, 1 number)
  isValidPassword: (password) => {
    const passwordRegex = /^(?=.*[A-Z])(?=.*\d).{8,}$/
    return passwordRegex.test(password)
  },

  // Validate phone number
  isValidPhone: (phone) => {
    const phoneRegex = /^[+]?[(]?[0-9]{3}[)]?[-\s]?[0-9]{3}[-\s]?[0-9]{4,6}$/
    return phoneRegex.test(phone)
  },

  // Check if string is not empty
  isNotEmpty: (str) => {
    return str && str.trim().length > 0
  },

  // Check minimum length
  minLength: (str, length) => {
    return str && str.length >= length
  },

  // Check maximum length
  maxLength: (str, length) => {
    return str && str.length <= length
  },

  // Check if string matches pattern
  matchesPattern: (str, pattern) => {
    const regex = new RegExp(pattern)
    return regex.test(str)
  },

  // Validate amount (positive number)
  isValidAmount: (amount) => {
    return !isNaN(amount) && amount > 0
  },

  // Trim and validate string
  isTrimmedAndValid: (str) => {
    return str && str.trim().length > 0 && str.trim() === str
  },
}

export default validationUtils