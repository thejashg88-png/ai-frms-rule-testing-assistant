import { STATUSES } from '../data/statuses'

// Falls back to DRAFT style so unknown rule statuses don't render blank badges.
export const getRuleStatusStyle = (status) => {
  const s = STATUSES.RULE_STATUS[status] ?? STATUSES.RULE_STATUS.DRAFT
  return { color: s.color, bgColor: s.bgColor, label: s.label }
}

// Falls back to a neutral grey style so any unrecognized execution status still renders.
export const getExecutionStatusStyle = (status) => {
  const s = STATUSES.EXECUTION_STATUS[status] ?? { label: status ?? 'Unknown', color: '#6b7280', bgColor: '#f3f4f6' }
  return { color: s.color, bgColor: s.bgColor, label: s.label }
}

// Returns badge colors for rule engine actions (ACCEPT / MONITOR / REJECT).
export const getActionStyle = (action) => {
  const map = {
    ACCEPT:  { color: '#16a34a', bgColor: '#dcfce7', label: 'Accept' },
    REJECT:  { color: '#dc2626', bgColor: '#fee2e2', label: 'Reject' },
    MONITOR: { color: '#ca8a04', bgColor: '#fef9c3', label: 'Monitor' },
  }
  return map[action] ?? { color: '#475569', bgColor: '#f1f5f9', label: action ?? '-' }
}
