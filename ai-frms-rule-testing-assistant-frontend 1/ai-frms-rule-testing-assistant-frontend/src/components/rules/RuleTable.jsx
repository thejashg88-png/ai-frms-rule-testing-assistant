import React from 'react'
import { useNavigate } from 'react-router-dom'
import Table from '../common/Table'
import Button from '../common/Button'
import Badge from '../common/Badge'
import RuleStatusBadge from './RuleStatusBadge'
import { getRuleTypeLabel } from '../../data/ruleTypes'

const ACTION_COLORS = {
  ACCEPT:  { bg: '#dcfce7', color: '#16a34a' },
  REJECT:  { bg: '#fee2e2', color: '#dc2626' },
  MONITOR: { bg: '#fef9c3', color: '#ca8a04' },
}

const RuleTable = ({ rules, loading, onDelete, deletingId, confirmId, onConfirmDelete, onCancelDelete }) => {
  const navigate = useNavigate()

  const columns = [
    {
      key: 'name',
      label: 'Rule',
      render: (val, row) => (
        <div>
          <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: 2 }}>{val}</div>
          {row.description && (
            <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{row.description}</div>
          )}
        </div>
      ),
    },
    {
      key: 'ruleType',
      label: 'Type',
      width: '120px',
      render: (val) => <Badge bgColor="#eff6ff" color="#2563eb" size="sm">{getRuleTypeLabel(val)}</Badge>,
    },
    {
      key: 'action',
      label: 'Action',
      width: '110px',
      render: (val) => {
        const c = ACTION_COLORS[val] ?? { bg: '#f1f5f9', color: '#475569' }
        return <Badge bgColor={c.bg} color={c.color} size="sm">{val}</Badge>
      },
    },
    {
      key: 'status',
      label: 'Status',
      width: '100px',
      render: (val) => <RuleStatusBadge status={val} />,
    },
    {
      key: 'createdAt',
      label: 'Created',
      width: '110px',
    },
    {
      key: 'id',
      label: 'Actions',
      width: '170px',
      render: (id) => {
        if (confirmId === id) {
          return (
            <div style={{ display: 'flex', gap: 6 }}>
              <Button variant="danger" size="sm" loading={deletingId === id} onClick={() => onConfirmDelete(id)}>
                Confirm
              </Button>
              <Button variant="ghost" size="sm" disabled={deletingId === id} onClick={onCancelDelete}>
                Cancel
              </Button>
            </div>
          )
        }
        return (
          <div style={{ display: 'flex', gap: 6 }}>
            <Button variant="outline" size="sm" onClick={() => navigate(`/rules/${id}/edit`)}>
              Edit
            </Button>
            <Button variant="ghost" size="sm" onClick={() => onDelete(id)}>
              Delete
            </Button>
          </div>
        )
      },
    },
  ]

  return (
    <Table
      columns={columns}
      data={rules}
      loading={loading}
      emptyMessage="No rules found. Create your first rule to get started."
    />
  )
}

export default RuleTable
