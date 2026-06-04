import React, { useState, useEffect, useCallback } from 'react'
import PageHeader from '../../components/common/PageHeader'
import Card from '../../components/common/Card'
import Table from '../../components/common/Table'
import Badge from '../../components/common/Badge'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import Select from '../../components/common/Select'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import Pagination from '../../components/common/Pagination'
import AuditLogDetailsModal from '../../components/audit/AuditLogDetailsModal'
import auditService from '../../services/auditService'
import '../../styles/pages.css'

const ACTION_OPTIONS = [
  { value: 'CREATE',          label: 'Create' },
  { value: 'UPDATE',          label: 'Update' },
  { value: 'DELETE',          label: 'Delete' },
  { value: 'INACTIVATE',      label: 'Inactivate' },
  { value: 'RUN_EXECUTION',   label: 'Run Execution' },
  { value: 'DOWNLOAD_REPORT', label: 'Download Report' },
]

const ENTITY_TYPE_OPTIONS = [
  { value: 'RULE',         label: 'Rule' },
  { value: 'SCENARIO',     label: 'Scenario' },
  { value: 'TEST_CASE',    label: 'Test Case' },
  { value: 'EXECUTION',    label: 'Execution' },
  { value: 'TRANSACTION',  label: 'Transaction' },
  { value: 'REPORT',       label: 'Report' },
  { value: 'AI_ASSISTANT', label: 'AI Assistant' },
]

const ACTION_BADGE = {
  CREATE:          { bg: '#dcfce7', color: '#16a34a' },
  UPDATE:          { bg: '#eff6ff', color: '#2563eb' },
  DELETE:          { bg: '#fee2e2', color: '#dc2626' },
  INACTIVATE:      { bg: '#ffedd5', color: '#c2410c' },
  RUN_EXECUTION:   { bg: '#f5f3ff', color: '#7c3aed' },
  DOWNLOAD_REPORT: { bg: '#f1f5f9', color: '#475569' },
}

const ENTITY_BADGE = {
  RULE:         { bg: '#eff6ff', color: '#2563eb' },
  SCENARIO:     { bg: '#f0fdfa', color: '#0d9488' },
  TEST_CASE:    { bg: '#f5f3ff', color: '#7c3aed' },
  EXECUTION:    { bg: '#ffedd5', color: '#c2410c' },
  TRANSACTION:  { bg: '#eef2ff', color: '#4338ca' },
  REPORT:       { bg: '#f8fafc', color: '#64748b' },
  AI_ASSISTANT: { bg: '#ecfeff', color: '#0891b2' },
}

const ActionBadge = ({ action }) => {
  const c = ACTION_BADGE[action] ?? { bg: '#f1f5f9', color: '#475569' }
  return <Badge bgColor={c.bg} color={c.color} size="sm">{action?.replace(/_/g, ' ') ?? '—'}</Badge>
}

const EntityBadge = ({ type }) => {
  const c = ENTITY_BADGE[type] ?? { bg: '#f1f5f9', color: '#475569' }
  return <Badge bgColor={c.bg} color={c.color} size="sm">{type?.replace(/_/g, ' ') ?? '—'}</Badge>
}

const AuditLogsPage = () => {
  const [logs, setLogs]             = useState([])
  const [totalItems, setTotalItems] = useState(0)
  const [loading, setLoading]       = useState(true)
  const [error, setError]           = useState(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize]     = useState(20)
  const [selectedLog, setSelectedLog] = useState(null)

  const [filters, setFilters]         = useState({ search: '', action: '', entityType: '' })
  const [appliedFilters, setAppliedFilters] = useState({ actor: '', action: '', entityType: '' })

  const load = useCallback(async (page, size, applied) => {
    setLoading(true)
    setError(null)
    try {
      const result = await auditService.getAll({
        page,
        size,
        actor:      applied.actor,
        action:     applied.action,
        entityType: applied.entityType,
      })
      setLogs(result.items)
      setTotalItems(result.totalItems)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load(currentPage, pageSize, appliedFilters)
  }, [currentPage, pageSize, appliedFilters, load])

  const handleFilterChange = (e) => {
    const { name, value } = e.target
    setFilters((prev) => ({ ...prev, [name]: value }))
  }

  const handleApplyFilters = () => {
    setAppliedFilters({ actor: filters.search, action: filters.action, entityType: filters.entityType })
    setCurrentPage(1)
  }

  const handleClearFilters = () => {
    setFilters({ search: '', action: '', entityType: '' })
    setAppliedFilters({ actor: '', action: '', entityType: '' })
    setCurrentPage(1)
  }

  const columns = [
    {
      key: 'createdAt',
      label: 'Date / Time',
      width: '160px',
      render: (val) => val ? new Date(val).toLocaleString() : '—',
    },
    {
      key: 'actor',
      label: 'Actor',
      width: '130px',
    },
    {
      key: 'action',
      label: 'Action',
      width: '150px',
      render: (val) => <ActionBadge action={val} />,
    },
    {
      key: 'entityType',
      label: 'Entity Type',
      width: '130px',
      render: (val) => <EntityBadge type={val} />,
    },
    {
      key: 'entityName',
      label: 'Entity Name',
    },
    {
      key: 'description',
      label: 'Description',
      render: (val) => (
        <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
          {val ? (val.length > 60 ? `${val.slice(0, 60)}…` : val) : '—'}
        </span>
      ),
    },
    {
      key: 'auditId',
      label: '',
      width: '110px',
      render: (_, row) => (
        <Button variant="ghost" size="sm" onClick={() => setSelectedLog(row)}>
          View Details
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Audit Logs"
        subtitle={`${totalItems} log${totalItems !== 1 ? 's' : ''} recorded`}
      />

      <div className="rules-filters">
        <div className="rules-search-wrapper">
          <Input
            name="search"
            placeholder="Search by actor or entity…"
            value={filters.search}
            onChange={handleFilterChange}
          />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select
            name="action"
            placeholder="All Actions"
            options={ACTION_OPTIONS}
            value={filters.action}
            onChange={handleFilterChange}
          />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select
            name="entityType"
            placeholder="All Entity Types"
            options={ENTITY_TYPE_OPTIONS}
            value={filters.entityType}
            onChange={handleFilterChange}
          />
        </div>
        <Button variant="primary" size="sm" onClick={handleApplyFilters}>
          Apply
        </Button>
        <Button variant="ghost" size="sm" onClick={handleClearFilters}>
          Clear
        </Button>
      </div>

      {error ? (
        <ErrorMessage
          title="Unable to load audit logs"
          message={error}
          onRetry={() => load(currentPage, pageSize, appliedFilters)}
        />
      ) : (
        <>
          {loading ? (
            <Loader message="Loading audit logs…" />
          ) : (
            <Card noPadding>
              <Table
                columns={columns}
                data={logs}
                emptyMessage="No audit log entries found."
              />
            </Card>
          )}

          {!loading && (
            <Pagination
              currentPage={currentPage}
              pageSize={pageSize}
              totalItems={totalItems}
              onPageChange={(page) => setCurrentPage(page)}
              onPageSizeChange={(size) => { setPageSize(size); setCurrentPage(1) }}
            />
          )}
        </>
      )}

      <AuditLogDetailsModal log={selectedLog} onClose={() => setSelectedLog(null)} />
    </div>
  )
}

export default AuditLogsPage
