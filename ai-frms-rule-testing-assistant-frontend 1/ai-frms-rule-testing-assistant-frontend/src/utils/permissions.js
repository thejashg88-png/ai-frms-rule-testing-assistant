export const ROLES = {
  ADMIN:  'ADMIN',
  TESTER: 'TESTER',
  VIEWER: 'VIEWER',
}

export const PERMISSIONS = {
  CREATE_RULE:   ['ADMIN'],
  EDIT_RULE:     ['ADMIN'],
  DELETE_RULE:   ['ADMIN'],

  CREATE_SCENARIO: ['ADMIN'],
  EDIT_SCENARIO:   ['ADMIN'],
  DELETE_SCENARIO: ['ADMIN'],

  CREATE_TEST_CASE: ['ADMIN', 'TESTER'],
  EDIT_TEST_CASE:   ['ADMIN', 'TESTER'],
  DELETE_TEST_CASE: ['ADMIN'],

  RUN_EXECUTION:    ['ADMIN', 'TESTER'],
  USE_AI_ASSISTANT: ['ADMIN', 'TESTER'],

  VIEW_DASHBOARD:  ['ADMIN', 'TESTER', 'VIEWER'],
  VIEW_RULES:      ['ADMIN', 'TESTER', 'VIEWER'],
  VIEW_SCENARIOS:  ['ADMIN', 'TESTER', 'VIEWER'],
  VIEW_TEST_CASES: ['ADMIN', 'TESTER', 'VIEWER'],
  VIEW_EXECUTIONS: ['ADMIN', 'TESTER', 'VIEWER'],
  VIEW_REPORTS:    ['ADMIN', 'TESTER', 'VIEWER'],

  DOWNLOAD_REPORTS:   ['ADMIN', 'TESTER'],
  VIEW_AUDIT_LOGS:    ['ADMIN'],
  CREATE_TRANSACTION: ['ADMIN'],
}

export const canAccess = (role, permission) => {
  if (!role) return false
  const allowed = PERMISSIONS[permission]?.includes(role) ?? false
  console.log('[PERMISSION CHECK]', permission, allowed)
  return allowed
}
