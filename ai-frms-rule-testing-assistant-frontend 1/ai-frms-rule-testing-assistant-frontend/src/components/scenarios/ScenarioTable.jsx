import React from 'react'
import { useNavigate } from 'react-router-dom'
import Table from '../common/Table'
import Button from '../common/Button'
import Badge from '../common/Badge'

const ScenarioTable = ({ scenarios, loading, onDelete, deletingId, confirmId, onConfirmDelete, onCancelDelete }) => {
  const navigate = useNavigate()

  const columns = [
    {
      key: 'name',
      label: 'Scenario',
      render: (val, row) => (
        <div>
          <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: 2 }}>{val}</div>
          {row.description && <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{row.description}</div>}
        </div>
      ),
    },
    {
      key: 'scenarioType',
      label: 'Type',
      width: '120px',
      render: (val) => <Badge bgColor="#eff6ff" color="#2563eb" size="sm">{val}</Badge>,
    },
    {
      key: 'testCaseCount',
      label: 'Test Cases',
      width: '100px',
      render: (val) => <span style={{ fontWeight: 600 }}>{val ?? 0}</span>,
    },
    {
      key: 'status',
      label: 'Status',
      width: '100px',
      render: (val) => {
        const isActive = val === 'ACTIVE'
        return <Badge bgColor={isActive ? '#d1fae5' : '#fee2e2'} color={isActive ? '#10b981' : '#ef4444'} size="sm">{val}</Badge>
      },
    },
    {
      key: 'createdAt',
      label: 'Created',
      width: '110px',
    },
    {
      key: 'id',
      label: 'Actions',
      width: '210px',
      render: (id) => {
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
            <Button variant="outline" size="sm" onClick={() => navigate(`/scenarios/${id}`)}>View</Button>
            <Button variant="ghost" size="sm" onClick={() => navigate(`/scenarios/${id}/edit`)}>Edit</Button>
            <Button variant="ghost" size="sm" onClick={() => onDelete(id)}>Delete</Button>
          </div>
        )
      },
    },
  ]

  return (
    <Table columns={columns} data={scenarios} loading={loading} emptyMessage="No scenarios found. Create your first test scenario." />
  )
}

export default ScenarioTable
