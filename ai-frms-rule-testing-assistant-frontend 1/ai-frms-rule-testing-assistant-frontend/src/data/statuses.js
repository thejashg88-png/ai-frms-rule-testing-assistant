export const STATUSES = {
  RULE_STATUS: {
    ACTIVE: {
      label: 'Active',
      value: 'ACTIVE',
      color: '#10b981',
      bgColor: '#d1fae5',
    },
    INACTIVE: {
      label: 'Inactive',
      value: 'INACTIVE',
      color: '#ef4444',
      bgColor: '#fee2e2',
    },
    DRAFT: {
      label: 'Draft',
      value: 'DRAFT',
      color: '#f59e0b',
      bgColor: '#fef3c7',
    },
    ARCHIVED: {
      label: 'Archived',
      value: 'ARCHIVED',
      color: '#6b7280',
      bgColor: '#f3f4f6',
    },
  },

  EXECUTION_STATUS: {
    PENDING: {
      label: 'Pending',
      value: 'PENDING',
      color: '#2563eb',
      bgColor: '#dbeafe',
    },
    RUNNING: {
      label: 'Running',
      value: 'RUNNING',
      color: '#3b82f6',
      bgColor: '#dbeafe',
    },
    PASSED: {
      label: 'Passed',
      value: 'PASSED',
      color: '#10b981',
      bgColor: '#d1fae5',
    },
    FAILED: {
      label: 'Failed',
      value: 'FAILED',
      color: '#ef4444',
      bgColor: '#fee2e2',
    },
    SKIPPED: {
      label: 'Skipped',
      value: 'SKIPPED',
      color: '#6b7280',
      bgColor: '#f3f4f6',
    },
  },
}

export const getStatusLabel = (type, status) => {
  return STATUSES[type]?.[status]?.label || status
}

export const getStatusColor = (type, status) => {
  return STATUSES[type]?.[status]?.color || '#6b7280'
}

export const getStatusBgColor = (type, status) => {
  return STATUSES[type]?.[status]?.bgColor || '#f3f4f6'
}

export default STATUSES