export const API_ENDPOINTS = {
  // Auth endpoints
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    VERIFY: '/auth/verify',
    REFRESH: '/auth/refresh',
    ME: '/auth/me',
  },

  // Rules endpoints
  RULES: {
    LIST: '/rules',
    GET: (id) => `/rules/${id}`,
    CREATE: '/rules',
    UPDATE: (id) => `/rules/${id}`,
    DELETE: (id) => `/rules/${id}`,
    STATS: '/rules/stats',
  },

  // Transactions endpoints
  TRANSACTIONS: {
    LIST: '/transactions',
    GET: (id) => `/transactions/${id}`,
    CREATE: '/transactions',
    UPDATE: (id) => `/transactions/${id}`,
    DELETE: (id) => `/transactions/${id}`,
    BULK_CREATE: '/transactions/bulk',
  },

  // Scenarios endpoints
  SCENARIOS: {
    LIST: '/scenarios',
    GET: (id) => `/scenarios/${id}`,
    CREATE: '/scenarios',
    UPDATE: (id) => `/scenarios/${id}`,
    DELETE: (id) => `/scenarios/${id}`,
  },

  // Test Cases endpoints
  TEST_CASES: {
    LIST: '/test-cases',
    GET: (id) => `/test-cases/${id}`,
    CREATE: '/test-cases',
    UPDATE: (id) => `/test-cases/${id}`,
    DELETE: (id) => `/test-cases/${id}`,
  },

  // Executions endpoints
  EXECUTIONS: {
    LIST: '/executions',
    GET: (id) => `/executions/${id}`,
    EXECUTE_TEST: '/executions/test',
    EXECUTE_SCENARIO: '/executions/scenario',
  },

  // Dashboard endpoints
  DASHBOARD: {
    SUMMARY: '/dashboard/summary',
    CHARTS: '/dashboard/charts',
  },
}

export default API_ENDPOINTS