import React from 'react'
import Badge from '../common/Badge'

const ACTION_COLORS = {
  ACCEPT:  { bg: '#dcfce7', color: '#16a34a' },
  REJECT:  { bg: '#fee2e2', color: '#dc2626' },
  MONITOR: { bg: '#fef9c3', color: '#ca8a04' },
}

const ExpectedResultBox = ({ expectedResult, expectedAction }) => {
  const ac = ACTION_COLORS[expectedAction] ?? { bg: '#f1f5f9', color: '#475569' }
  const isPass = expectedResult === 'PASS'

  return (
    <div style={{ background: 'var(--bg-secondary)', borderRadius: 8, padding: '16px', border: '1px solid var(--border)' }}>
      <p style={{ margin: '0 0 12px', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
        Expected Result
      </p>
      <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
        <Badge bgColor={isPass ? '#dcfce7' : '#fee2e2'} color={isPass ? '#16a34a' : '#dc2626'}>
          {expectedResult ?? '—'}
        </Badge>
        {expectedAction && (
          <Badge bgColor={ac.bg} color={ac.color}>
            Action: {expectedAction}
          </Badge>
        )}
      </div>
    </div>
  )
}

export default ExpectedResultBox
