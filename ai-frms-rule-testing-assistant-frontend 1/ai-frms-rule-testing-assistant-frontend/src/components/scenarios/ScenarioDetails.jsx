import React from 'react'
import Badge from '../common/Badge'

const Row = ({ label, value }) => (
  <div style={{ display: 'flex', gap: 16, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
    <span style={{ width: 180, flexShrink: 0, fontSize: 13, color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
    <span style={{ fontSize: 14, color: 'var(--text-primary)' }}>{value ?? '—'}</span>
  </div>
)

const ScenarioDetails = ({ scenario }) => {
  if (!scenario) return null
  const isActive = scenario.status === 'ACTIVE'

  return (
    <div>
      <Row label="Scenario Name" value={<strong>{scenario.name}</strong>} />
      <Row label="Description" value={scenario.description || '—'} />
      <Row label="Scenario Type" value={
        <Badge bgColor="#eff6ff" color="#2563eb" size="sm">{scenario.scenarioType}</Badge>
      } />
      <Row label="Test Cases" value={<strong>{scenario.testCaseCount ?? 0}</strong>} />
      <Row label="Status" value={
        <Badge bgColor={isActive ? '#d1fae5' : '#fee2e2'} color={isActive ? '#10b981' : '#ef4444'} size="sm">
          {scenario.status}
        </Badge>
      } />
      <Row label="Created" value={scenario.createdAt} />
    </div>
  )
}

export default ScenarioDetails
