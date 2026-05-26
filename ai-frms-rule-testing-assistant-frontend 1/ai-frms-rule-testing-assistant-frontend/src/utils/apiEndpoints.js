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
    LIST: '/testcases',
    GET: (id) => `/testcases/${id}`,
    CREATE: '/testcases',
    UPDATE: (id) => `/testcases/${id}`,
    DELETE: (id) => `/testcases/${id}`,
  },

  // Executions endpoints
  EXECUTIONS: {
    LIST: '/executions',
    GET: (id) => `/executions/${id}`,
    RUN_TEST_CASE: (id) => `/executions/run-testcase/${id}`,
    RUN_SCENARIO: (id) => `/executions/run-scenario/${id}`,
    STATS: '/executions/stats',
  },

  // Dashboard endpoints
  DASHBOARD: {
    SUMMARY: '/dashboard/summary',
    RECENT_EXECUTIONS: '/dashboard/recent-executions',
  },

  // Reports endpoints
  REPORTS: {
    SUMMARY: '/reports/summary',
    EXECUTIONS: '/reports/executions',
  },

  // AI endpoints
  AI: {
    GENERATE_TEST_CASES: '/ai/generate-test-cases',
    EXPLAIN_RULE: '/ai/explain-rule',
    ANALYZE_FAILURE: '/ai/analyze-failure',
    GENERATE_TRANSACTION: '/ai/generate-transaction',
  },
}

export default API_ENDPOINTS