import React from 'react'
import Badge from '../common/Badge'

const STATUS_STYLE = {
  PASSED:  { bg: '#dcfce7', color: '#16a34a' },
  FAILED:  { bg: '#fee2e2', color: '#dc2626' },
  RUNNING: { bg: '#dbeafe', color: '#2563eb' },
  PENDING: { bg: '#fef9c3', color: '#ca8a04' },
}

const PassFailBadge = ({ status, size = 'sm' }) => {
  const s = STATUS_STYLE[status] ?? { bg: '#f3f4f6', color: '#6b7280' }
  return <Badge bgColor={s.bg} color={s.color} size={size}>{status ?? '—'}</Badge>
}

export default PassFailBadge
