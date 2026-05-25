import React from 'react'
import Table from '../common/Table'
import Badge from '../common/Badge'
import { STATUSES } from '../../data/statuses'

const RecentExecutions = ({ executions = [], loading = false }) => {
  const columns = [
    {
      key: 'entityName',
      label: 'Name',
      render: (val, row) => (
        <div>
          <div style={{ fontWeight: 500, fontSize: 13 }}>{val}</div>
          <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{row.executionType}</div>
        </div>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      width: '90px',
      render: (val) => {
        const s = STATUSES.EXECUTION_STATUS[val] ?? { label: val, color: '#6b7280', bgColor: '#f3f4f6' }
        return <Badge bgColor={s.bgColor} color={s.color} size="sm">{s.label}</Badge>
      },
    },
    {
      key: 'executedAt',
      label: 'When',
      width: '110px',
      render: (val) => val ? new Date(val).toLocaleDateString() : '—',
    },
  ]

  return (
    <Table
      columns={columns}
      data={executions}
      loading={loading}
      emptyMessage="No executions yet"
    />
  )
}

export default RecentExecutions
