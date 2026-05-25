import React from 'react'
import { useNavigate } from 'react-router-dom'
import Table from '../common/Table'
import Button from '../common/Button'
import Badge from '../common/Badge'
import PassFailBadge from './PassFailBadge'

const ACTION_COLORS = {
  ACCEPT:  { bg: '#dcfce7', color: '#16a34a' },
  REJECT:  { bg: '#fee2e2', color: '#dc2626' },
  MONITOR: { bg: '#fef9c3', color: '#ca8a04' },
}

const ExecutionResultTable = ({ executions, loading }) => {
  const navigate = useNavigate()

  const columns = [
    {
      key: 'entityName',
      label: 'Name',
      render: (val, row) => (
        <div>
          <div style={{ fontWeight: 600, marginBottom: 2 }}>{val}</div>
          <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{row.executionType}</div>
        </div>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      width: '90px',
      render: (val) => <PassFailBadge status={val} />,
    },
    {
      key: 'result',
      label: 'Result',
      width: '100px',
      render: (val) => {
        if (!val) return <span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>—</span>
        const c = ACTION_COLORS[val] ?? { bg: '#f1f5f9', color: '#475569' }
        return <Badge bgColor={c.bg} color={c.color} size="sm">{val}</Badge>
      },
    },
    {
      key: 'durationMs',
      label: 'Duration',
      width: '90px',
      render: (val) => val != null ? `${val}ms` : '—',
    },
    {
      key: 'executedAt',
      label: 'Executed',
      width: '130px',
      render: (val) => val ? new Date(val).toLocaleString() : '—',
    },
    {
      key: 'id',
      label: '',
      width: '80px',
      render: (id) => (
        <Button variant="outline" size="sm" onClick={() => navigate(`/executions/${id}`)}>View</Button>
      ),
    },
  ]

  return (
    <Table columns={columns} data={executions} loading={loading} emptyMessage="No executions yet." />
  )
}

export default ExecutionResultTable
