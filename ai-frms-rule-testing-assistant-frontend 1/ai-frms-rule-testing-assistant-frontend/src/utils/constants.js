// API Configuration
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
export const API_TIMEOUT = 10000

// Pagination
export const DEFAULT_PAGE_SIZE = 10
export const PAGE_SIZES = [10, 25, 50, 100]

// Storage Keys
export const STORAGE_KEYS = {
  AUTH_TOKEN: 'auth_token',
  USER_DATA: 'user_data',
  RECENT_FILTERS: 'recent_filters',
  USER_PREFERENCES: 'user_preferences',
}

// Toast Duration (ms)
export const TOAST_DURATION = {
  SHORT: 2000,
  NORMAL: 3000,
  LONG: 5000,
  PERSISTENT: 0,
}

// Toast Types
export const TOAST_TYPES = {
  SUCCESS: 'success',
  ERROR: 'error',
  WARNING: 'warning',
  INFO: 'info',
}

// Application Title
export const APP_TITLE = 'AI FRMS Rule Testing Assistant'
export const APP_VERSION = '0.0.0'

// Date Format
export const DATE_FORMAT = 'DD/MM/YYYY'
export const DATETIME_FORMAT = 'DD/MM/YYYY HH:mm:ss'
export const TIME_FORMAT = 'HH:mm:ss'

// Rule Status
export const RULE_STATUS = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  DRAFT: 'DRAFT',
  ARCHIVED: 'ARCHIVED',
}

// Execution Status
export const EXECUTION_STATUS = {
  PENDING: 'PENDING',
  RUNNING: 'RUNNING',
  PASSED: 'PASSED',
  FAILED: 'FAILED',
  SKIPPED: 'SKIPPED',
}

export default {
  API_BASE_URL,
  API_TIMEOUT,
  DEFAULT_PAGE_SIZE,
  PAGE_SIZES,
  STORAGE_KEYS,
  TOAST_DURATION,
  TOAST_TYPES,
  APP_TITLE,
  APP_VERSION,
}