import React from 'react'
import { useNavigate } from 'react-router-dom'
import Table from '../common/Table'
import Button from '../common/Button'
import Badge from '../common/Badge'
import { STATUSES } from '../../data/statuses'

const ACTION_COLORS = {
  ACCEPT:  { bg: '#dcfce7', color: '#16a34a' },
  REJECT:  { bg: '#fee2e2', color: '#dc2626' },
  MONITOR: { bg: '#fef9c3', color: '#ca8a04' },
}

const TestCaseTable = ({ testCases, loading, onDelete, deletingId, confirmId, onConfirmDelete, onCancelDelete }) => {
  const navigate = useNavigate()

  const columns = [
    {
      key: 'name',
      label: 'Test Case',
      render: (val, row) => (
        <div>
          <div style={{ fontWeight: 600, marginBottom: 2 }}>{val}</div>
          {row.scenarioName && <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Scenario: {row.scenarioName}</div>}
        </div>
      ),
    },
    {
      key: 'ruleName',
      label: 'Rule',
      width: '160px',
      render: (val) => <span style={{ fontSize: 13 }}>{val ?? '—'}</span>,
    },
    {
      key: 'expectedAction',
      label: 'Expected',
      width: '100px',
      render: (val) => {
        const c = ACTION_COLORS[val] ?? { bg: '#f1f5f9', color: '#475569' }
        return <Badge bgColor={c.bg} color={c.color} size="sm">{val}</Badge>
      },
    },
    {
      key: 'lastExecutionStatus',
      label: 'Last Run',
      width: '90px',
      render: (val) => {
        if (!val) return <span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>Not run</span>
        const s = STATUSES.EXECUTION_STATUS[val] ?? { label: val, color: '#6b7280', bgColor: '#f3f4f6' }
        return <Badge bgColor={s.bgColor} color={s.color} size="sm">{s.label}</Badge>
      },
    },
    {
      key: 'id',
      label: 'Actions',
      width: '220px',
      render: (id, row) => {
        if (confirmId === id) {
          return (
            <div style={{ display: 'flex', gap: 6 }}>
              <Button variant="danger" size="sm" loading={deletingId === id} onClick={() => onConfirmDelete(id)}>Confirm</Button>
              <Button variant="ghost" size="sm" disabled={deletingId === id} onClick={onCancelDelete}>Cancel</Button>
            </div>
          )
        }
        return (
          <div style={{ display: 'flex', gap: 6 }}>
            <Button variant="primary" size="sm" onClick={() => navigate(`/testcases/${id}`)}>View</Button>
            <Button variant="outline" size="sm" onClick={() => navigate(`/testcases/${id}/edit`)}>Edit</Button>
            <Button variant="ghost" size="sm" onClick={() => onDelete(id)}>Delete</Button>
          </div>
        )
      },
    },
  ]

  return (
    <Table columns={columns} data={testCases} loading={loading} emptyMessage="No test cases found." />
  )
}

export default TestCaseTable
