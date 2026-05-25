import React from 'react'
import Badge from '../common/Badge'
import TestInputDataBox from './TestInputDataBox'
import ExpectedResultBox from './ExpectedResultBox'
import { STATUSES } from '../../data/statuses'

const Row = ({ label, value }) => (
  <div style={{ display: 'flex', gap: 16, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
    <span style={{ width: 180, flexShrink: 0, fontSize: 13, color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
    <span style={{ fontSize: 14, color: 'var(--text-primary)' }}>{value ?? '—'}</span>
  </div>
)

const TestCaseDetails = ({ testCase }) => {
  if (!testCase) return null
  const lastExec = testCase.lastExecutionStatus
  const execStatus = lastExec ? STATUSES.EXECUTION_STATUS[lastExec] : null

  return (
    <div>
      <Row label="Name" value={<strong>{testCase.name}</strong>} />
      <Row label="Description" value={testCase.description} />
      <Row label="Scenario" value={testCase.scenarioName} />
      <Row label="Rule" value={testCase.ruleName} />
      <Row label="Status" value={
        <Badge bgColor={testCase.status === 'ACTIVE' ? '#d1fae5' : '#fee2e2'} color={testCase.status === 'ACTIVE' ? '#10b981' : '#ef4444'} size="sm">
          {testCase.status}
        </Badge>
      } />
      {execStatus && (
        <Row label="Last Execution" value={
          <Badge bgColor={execStatus.bgColor} color={execStatus.color} size="sm">{execStatus.label}</Badge>
        } />
      )}
      <Row label="Created" value={testCase.createdAt} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 20 }}>
        <TestInputDataBox inputData={testCase.inputData ?? {}} />
        <ExpectedResultBox expectedResult={testCase.expectedResult} expectedAction={testCase.expectedAction} />
      </div>
    </div>
  )
}

export default TestCaseDetails
