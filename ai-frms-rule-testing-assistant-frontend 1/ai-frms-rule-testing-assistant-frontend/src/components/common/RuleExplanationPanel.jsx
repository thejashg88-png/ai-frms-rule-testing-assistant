import React from 'react'

// Extracts ruleExplanation from a result object.
// Backend may place it directly on the result or nested inside comparisonResult.
export const getRuleExplanation = (result) =>
  result?.ruleExplanation ||
  result?.comparisonResult?.ruleExplanation ||
  null

const FIELD_DEFS = [
  { key: 'ruleType',            label: 'Rule Type' },
  { key: 'expectedAction',      label: 'Expected Action' },
  { key: 'actualAction',        label: 'Actual Action' },
  { key: 'matchedCount',        label: 'Matched Count' },
  { key: 'historicalCount',     label: 'Historical Count' },
  { key: 'currentCount',        label: 'Current Count' },
  { key: 'requiredCount',       label: 'Required Count' },
  { key: 'frequencyWindow',     label: 'Frequency Window' },
  { key: 'actualAmount',        label: 'Actual Amount' },
  { key: 'maxAmount',           label: 'Max Amount' },
  { key: 'averageAmount',       label: 'Average Amount' },
  { key: 'thresholdAmount',     label: 'Threshold Amount' },
  { key: 'percentageThreshold', label: 'Percentage Threshold' },
  { key: 'ruleReason',          label: 'Rule Reason' },
  { key: 'resultExplanation',   label: 'Result Explanation' },
]

const RuleExplanationPanel = ({ explanation, title }) => {
  if (!explanation) return null

  console.log('[Rule Explanation]', explanation)

  const triggered = explanation.triggered === true
  const panelTitle = title ?? (triggered ? 'Why Rule Triggered' : 'Why Rule Did Not Trigger')

  const accentColor  = triggered ? '#10b981' : '#3b82f6'
  const headerBg     = triggered ? 'rgba(16, 185, 129, 0.10)' : 'rgba(59, 130, 246, 0.10)'
  const titleColor   = triggered ? '#059669' : '#2563eb'

  return (
    <div style={{
      marginTop: 16,
      border: `1px solid ${accentColor}`,
      borderRadius: 10,
      overflow: 'hidden',
    }}>
      <div style={{
        padding: '10px 16px',
        background: headerBg,
        borderBottom: `1px solid ${accentColor}`,
        display: 'flex',
        alignItems: 'center',
        gap: 8,
      }}>
        <span style={{ fontSize: 14, fontWeight: 700, color: titleColor }}>
          {triggered ? '✓' : '✗'} {panelTitle}
        </span>
      </div>

      <div style={{ padding: '12px 16px', background: 'var(--bg-secondary)' }}>
        {FIELD_DEFS.map(({ key, label }) => {
          const val = explanation[key]
          if (val === null || val === undefined || val === '') return null
          return (
            <div key={key} style={{ display: 'flex', gap: 12, padding: '4px 0', fontSize: 13 }}>
              <span style={{ width: 180, flexShrink: 0, color: 'var(--text-secondary)', fontWeight: 500 }}>
                {label}
              </span>
              <span style={{ color: 'var(--text-primary)', flex: 1 }}>
                {String(val)}
              </span>
            </div>
          )
        })}

        {/* Always show Matched Transactions section — shows list or empty message */}
        <div style={{ paddingTop: 6, marginTop: 4, borderTop: '1px solid var(--border)' }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>
            Matched Transactions
          </div>
          {Array.isArray(explanation.matchedTransactions) && explanation.matchedTransactions.length > 0 ? (
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              {explanation.matchedTransactions.map((txn, i) => (
                <li key={i} style={{ fontSize: 12, color: 'var(--text-primary)', padding: '2px 0' }}>
                  {typeof txn === 'string' ? txn : JSON.stringify(txn)}
                </li>
              ))}
            </ul>
          ) : (
            <span style={{ fontSize: 12, color: 'var(--text-secondary)', fontStyle: 'italic' }}>
              No matched historical transactions found.
            </span>
          )}
        </div>
      </div>
    </div>
  )
}

export default RuleExplanationPanel
