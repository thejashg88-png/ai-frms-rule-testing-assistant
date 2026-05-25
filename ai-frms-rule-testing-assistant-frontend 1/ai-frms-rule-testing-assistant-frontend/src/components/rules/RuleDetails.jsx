import React from 'react'
import Badge from '../common/Badge'
import RuleStatusBadge from './RuleStatusBadge'
import { RULE_TYPE_MAP } from '../../data/ruleTypes'
import { ACTION_MAP } from '../../data/actions'

const Row = ({ label, value }) => (
  <div style={{ display: 'flex', gap: 16, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
    <span style={{ width: 180, flexShrink: 0, fontSize: 13, color: 'var(--text-secondary)', fontWeight: 500 }}>
      {label}
    </span>
    <span style={{ fontSize: 14, color: 'var(--text-primary)' }}>{value ?? '—'}</span>
  </div>
)

const RuleDetails = ({ rule }) => {
  if (!rule) return null
  const actionStyle = ACTION_MAP[rule.action] ?? { color: '#475569', bgColor: '#f1f5f9', label: rule.action }

  return (
    <div>
      <Row label="Rule Name" value={<strong>{rule.name}</strong>} />
      <Row label="Description" value={rule.description || '—'} />
      <Row label="Rule Type" value={
        <Badge bgColor="#eff6ff" color="#2563eb" size="sm">
          {RULE_TYPE_MAP[rule.ruleType] ?? rule.ruleType}
        </Badge>
      } />
      <Row label="Action" value={
        <Badge bgColor={actionStyle.bgColor} color={actionStyle.color} size="sm">
          {actionStyle.label ?? rule.action}
        </Badge>
      } />
      <Row label="Status" value={<RuleStatusBadge status={rule.status} />} />
      <Row label="Transaction Count" value={rule.txnCount} />
      <Row label="Max Amount" value={rule.maxAmount != null ? `$${rule.maxAmount.toLocaleString()}` : null} />
      <Row label="Transaction Amount" value={rule.txnAmount != null ? `$${rule.txnAmount.toLocaleString()}` : null} />
      <Row label="Frequency" value={rule.frequency} />
      <Row label="Percentage Threshold" value={rule.percentageThreshold != null ? `${rule.percentageThreshold}%` : null} />
      <Row label="Created" value={rule.createdAt} />
    </div>
  )
}

export default RuleDetails
