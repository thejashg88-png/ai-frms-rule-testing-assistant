import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import Select from '../../components/common/Select'
import Badge from '../../components/common/Badge'
import Card from '../../components/common/Card'
import Table from '../../components/common/Table'
import ErrorMessage from '../../components/common/ErrorMessage'
import { useToast } from '../../hooks/useToast'
import ruleService from '../../services/ruleService'
import { STATUSES } from '../../data/statuses'
import '../../styles/pages.css'

const ACTION_COLORS = {
  ACCEPT:  { bg: '#dcfce7', color: '#16a34a' },
  REJECT:  { bg: '#fee2e2', color: '#dc2626' },
  MONITOR: { bg: '#fef9c3', color: '#ca8a04' },
}

const STATUS_FILTER_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const TYPE_FILTER_OPTIONS = [
  { value: 'CREDIT',      label: 'Credit' },
  { value: 'AMOUNT',      label: 'Amount' },
  { value: 'VELOCITY',    label: 'Velocity' },
  { value: 'GEO',         label: 'Geographic' },
  { value: 'FRAUD',       label: 'Fraud' },
  { value: 'CARD',        label: 'Card' },
  { value: 'TRANSACTION', label: 'Transaction' },
  { value: 'FREQUENCY',   label: 'Frequency' },
]

const RulesPage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()

  const [rules, setRules] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [confirmId, setConfirmId] = useState(null)
  const [filters, setFilters] = useState({ search: '', status: '', ruleType: '' })

  const loadRules = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await ruleService.getAll(filters)
      setRules(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => { loadRules() }, [loadRules])

  const handleFilterChange = (e) => {
    const { name, value } = e.target
    setFilters((p) => ({ ...p, [name]: value }))
  }

  const handleDelete = async (id) => {
    setDeleting(id)
    try {
      await ruleService.delete(id)
      setRules((prev) => prev.filter((r) => r.id !== id))
      addToast('Rule deleted successfully', 'success')
    } catch (err) {
      addToast(err.message, 'error')
    } finally {
      setDeleting(null)
      setConfirmId(null)
    }
  }

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
      render: (val) => (
        <Badge bgColor="#eff6ff" color="#2563eb" size="sm">{val}</Badge>
      ),
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
      render: (val) => {
        const s = STATUSES.RULE_STATUS[val] ?? STATUSES.RULE_STATUS.DRAFT
        return <Badge bgColor={s.bgColor} color={s.color} size="sm">{s.label}</Badge>
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
      width: '170px',
      render: (id) => {
        if (confirmId === id) {
          return (
            <div style={{ display: 'flex', gap: 6 }}>
              <Button
                variant="danger"
                size="sm"
                loading={deleting === id}
                onClick={() => handleDelete(id)}
              >
                Confirm
              </Button>
              <Button
                variant="ghost"
                size="sm"
                disabled={deleting === id}
                onClick={() => setConfirmId(null)}
              >
                Cancel
              </Button>
            </div>
          )
        }
        return (
          <div style={{ display: 'flex', gap: 6 }}>
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate(`/rules/${id}/edit`)}
            >
              Edit
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setConfirmId(id)}
            >
              Delete
            </Button>
          </div>
        )
      },
    },
  ]

  return (
    <div className="rules-page">
      <PageHeader
        title="Rules"
        subtitle={`${rules.length} rule${rules.length !== 1 ? 's' : ''} in the system`}
        actions={
          <Button variant="primary" onClick={() => navigate('/rules/create')}>
            + Create Rule
          </Button>
        }
      />

      {/* ── Filters ── */}
      <div className="rules-filters">
        <div className="rules-search-wrapper">
          <Input
            name="search"
            placeholder="Search by name or description…"
            value={filters.search}
            onChange={handleFilterChange}
          />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select
            name="status"
            placeholder="All Statuses"
            options={STATUS_FILTER_OPTIONS}
            value={filters.status}
            onChange={handleFilterChange}
          />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select
            name="ruleType"
            placeholder="All Types"
            options={TYPE_FILTER_OPTIONS}
            value={filters.ruleType}
            onChange={handleFilterChange}
          />
        </div>
      </div>

      {/* ── Content ── */}
      {error ? (
        <ErrorMessage
          title="Failed to load rules"
          message={error}
          onRetry={loadRules}
        />
      ) : (
        <Card noPadding>
          <Table
            columns={columns}
            data={rules}
            loading={loading}
            emptyMessage="No rules found. Create your first rule to get started."
          />
        </Card>
      )}
    </div>
  )
}

export default RulesPage
