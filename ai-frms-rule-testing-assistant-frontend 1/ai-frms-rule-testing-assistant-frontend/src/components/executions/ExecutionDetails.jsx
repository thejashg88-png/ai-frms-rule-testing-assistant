import React from 'react'
import PassFailBadge from './PassFailBadge'
import FailureReasonBox from './FailureReasonBox'
import Badge from '../common/Badge'
import RuleExplanationPanel, { getRuleExplanation } from '../common/RuleExplanationPanel'

const ACTION_COLORS = {
  ACCEPT:  { bg: '#dcfce7', color: '#16a34a' },
  REJECT:  { bg: '#fee2e2', color: '#dc2626' },
  MONITOR: { bg: '#fef9c3', color: '#ca8a04' },
}

const Row = ({ label, value }) => (
  <div style={{ display: 'flex', gap: 16, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
    <span style={{ width: 180, flexShrink: 0, fontSize: 13, color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
    <span style={{ fontSize: 14, color: 'var(--text-primary)' }}>{value ?? '—'}</span>
  </div>
)

const ExecutionDetails = ({ execution }) => {
  if (!execution) return null
  const ac = ACTION_COLORS[execution.result] ?? null

  return (
    <div>
      <Row label="Entity Name" value={<strong>{execution.entityName}</strong>} />
      <Row label="Execution Type" value={execution.executionType} />
      <Row label="Status" value={<PassFailBadge status={execution.status} />} />
      <Row label="Result (Action)" value={
        ac && execution.result
          ? <Badge bgColor={ac.bg} color={ac.color}>{execution.result}</Badge>
          : '—'
      } />
      <Row label="Duration" value={execution.durationMs != null ? `${execution.durationMs}ms` : null} />
      <Row label="Executed At" value={execution.executedAt ? new Date(execution.executedAt).toLocaleString() : null} />
      <FailureReasonBox reason={execution.failureReason} />
      <RuleExplanationPanel explanation={getRuleExplanation(execution.results?.[0])} />
    </div>
  )
}

export default ExecutionDetails
