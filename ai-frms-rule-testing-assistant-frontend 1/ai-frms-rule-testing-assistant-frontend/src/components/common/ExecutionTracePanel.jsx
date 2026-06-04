import React from 'react'

export const getExecutionTrace = (result) => {
  const trace =
    result?.executionTrace ||
    result?.results?.[0]?.executionTrace ||
    result?.comparisonResult?.executionTrace ||
    result?.ruleExplanation?.executionTrace ||
    null
  console.log('[getExecutionTrace] result keys:', result ? Object.keys(result) : 'null')
  console.log('[getExecutionTrace] found trace:', trace)
  return Array.isArray(trace) && trace.length > 0 ? trace : []
}

const STATUS_STYLES = {
  SUCCESS: { bg: '#dcfce7', color: '#16a34a' },
  INFO:    { bg: 'rgba(59, 130, 246, 0.12)', color: '#2563eb' },
  FAILED:  { bg: '#fee2e2', color: '#dc2626' },
  ERROR:   { bg: '#fee2e2', color: '#dc2626' },
  WARNING: { bg: '#fef9c3', color: '#ca8a04' },
}

const StatusBadge = ({ status }) => {
  const s = String(status || '').toUpperCase()
  const style = STATUS_STYLES[s] ?? { bg: '#f1f5f9', color: '#475569' }
  return (
    <span style={{
      padding: '2px 8px', borderRadius: 4, fontSize: 11, fontWeight: 700,
      background: style.bg, color: style.color, flexShrink: 0, letterSpacing: '0.03em',
    }}>
      {s}
    </span>
  )
}

const ExecutionTracePanel = ({ trace, title }) => {
  if (!Array.isArray(trace) || trace.length === 0) return null
  console.log('[Execution Trace]', trace)

  const sorted = [...trace].sort((a, b) => (a.stepNumber ?? 0) - (b.stepNumber ?? 0))
  const panelTitle = title ?? 'Execution Trace'

  return (
    <div style={{ marginTop: 16, border: '1px solid #6366f1', borderRadius: 10, overflow: 'hidden' }}>
      <div style={{
        padding: '10px 16px',
        background: 'rgba(99, 102, 241, 0.10)',
        borderBottom: '1px solid #6366f1',
        display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <span style={{ fontSize: 14, fontWeight: 700, color: '#4f46e5' }}>
          {panelTitle}
        </span>
        <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
          {sorted.length} step{sorted.length !== 1 ? 's' : ''}
        </span>
      </div>
      <div style={{ background: 'var(--bg-secondary)', padding: '4px 0' }}>
        {sorted.map((step, i) => {
          const detailStr =
            step.detail == null        ? '' :
            typeof step.detail === 'string' ? step.detail :
            JSON.stringify(step.detail)

          return (
            <div key={i} style={{
              display: 'flex', gap: 12, padding: '8px 16px',
              borderBottom: i < sorted.length - 1 ? '1px solid var(--border)' : 'none',
              alignItems: 'flex-start',
            }}>
              <div style={{
                minWidth: 24, height: 24, borderRadius: '50%',
                background: 'rgba(99, 102, 241, 0.15)',
                color: '#4f46e5',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 11, fontWeight: 700, flexShrink: 0, marginTop: 1,
              }}>
                {step.stepNumber ?? i + 1}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: detailStr ? 3 : 0, flexWrap: 'wrap' }}>
                  <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', flex: 1 }}>
                    {step.title}
                  </span>
                  <StatusBadge status={step.status} />
                </div>
                {detailStr && (
                  <p style={{ margin: 0, fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                    {detailStr}
                  </p>
                )}
                {step.ruleType && (
                  <span style={{ display: 'inline-block', marginTop: 2, fontSize: 11, color: 'var(--text-secondary)', fontStyle: 'italic' }}>
                    {step.ruleType}
                  </span>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default ExecutionTracePanel
