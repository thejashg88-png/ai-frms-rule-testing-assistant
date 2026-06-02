import React from 'react'
import Badge from '../common/Badge'

const ACTION_COLORS = {
  ACCEPT:  { bg: '#dcfce7', color: '#16a34a' },
  REJECT:  { bg: '#fee2e2', color: '#dc2626' },
  MONITOR: { bg: '#fef9c3', color: '#ca8a04' },
}

// expectedResult can be either a plain string ("PASS"/"FAIL") from older test cases
// or a full ExpectedResult object from the backend. Both shapes must render safely.
function getExpectedResultLabel(expectedResult) {
  if (!expectedResult) return 'N/A'
  if (typeof expectedResult === 'string') return expectedResult
  if (typeof expectedResult === 'object') {
    return (
      expectedResult.expectedAction ||
      expectedResult.expectedOutcome ||
      expectedResult.expectedEvaluationStatus ||
      'N/A'
    )
  }
  return String(expectedResult)
}

function renderValue(value) {
  if (value === null || value === undefined || value === '') return 'N/A'
  if (Array.isArray(value)) return value.length ? value.join(', ') : 'N/A'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const Field = ({ label, value }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '7px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
    <span style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
    <span style={{ color: 'var(--text-primary)' }}>{renderValue(value)}</span>
  </div>
)

const ExpectedResultBox = ({ expectedResult, expectedAction }) => {
  console.log('[ExpectedResultBox] expectedResult', expectedResult)

  const isObject = expectedResult !== null && typeof expectedResult === 'object'
  const label    = getExpectedResultLabel(expectedResult)

  const action = expectedAction ||
    (isObject ? expectedResult.expectedAction : null)
  const ac = ACTION_COLORS[action] ?? { bg: '#f1f5f9', color: '#475569' }

  const isPass = label === 'PASS' || label === 'ACCEPT'

  return (
    <div style={{ background: 'var(--bg-secondary)', borderRadius: 8, padding: '16px', border: '1px solid var(--border)' }}>
      <p style={{ margin: '0 0 12px', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
        Expected Result
      </p>

      {/* Summary badges */}
      <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: isObject ? 12 : 0 }}>
        <Badge bgColor={isPass ? '#dcfce7' : '#fee2e2'} color={isPass ? '#16a34a' : '#dc2626'}>
          {label}
        </Badge>
        {action && (
          <Badge bgColor={ac.bg} color={ac.color}>
            Action: {action}
          </Badge>
        )}
      </div>

      {/* Expanded fields when expectedResult is an object */}
      {isObject && (
        <div>
          <Field label="Outcome"            value={expectedResult.expectedOutcome} />
          <Field label="Action"             value={expectedResult.expectedAction} />
          <Field label="Risk Level"         value={expectedResult.expectedRiskLevel} />
          <Field label="Evaluation Status"  value={expectedResult.expectedEvaluationStatus} />
          <Field label="Risk Score"         value={expectedResult.expectedRiskScore} />
          <Field label="Rule Type"          value={expectedResult.expectedRuleType} />
          <Field label="Alert Codes"        value={expectedResult.expectedAlertCodes} />
          <Field label="Remarks"            value={expectedResult.remarks} />
        </div>
      )}
    </div>
  )
}

export default ExpectedResultBox
