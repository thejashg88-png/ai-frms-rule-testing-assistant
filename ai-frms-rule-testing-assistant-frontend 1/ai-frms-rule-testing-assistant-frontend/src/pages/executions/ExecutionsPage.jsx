import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import Select from '../../components/common/Select'
import Card from '../../components/common/Card'
import ErrorMessage from '../../components/common/ErrorMessage'
import Pagination from '../../components/common/Pagination'
import ExecutionResultTable from '../../components/executions/ExecutionResultTable'
import executionService from '../../services/executionService'
import useClientPagination from '../../hooks/useClientPagination'
import { useAuth } from '../../hooks/useAuth'
import '../../styles/pages.css'

const STATUS_OPTIONS = [
  { value: 'ALL',     label: 'All Statuses' },
  { value: 'PASSED',  label: 'Passed' },
  { value: 'FAILED',  label: 'Failed' },
  { value: 'PENDING', label: 'Pending' },
]

const TYPE_OPTIONS = [
  { value: 'ALL',       label: 'All Types' },
  { value: 'TEST_CASE', label: 'Test Case' },
  { value: 'SCENARIO',  label: 'Scenario' },
]

const ExecutionsPage = () => {
  const navigate = useNavigate()
  const { isAdmin, isTester } = useAuth()

  const [allExecutions, setAllExecutions] = useState([])
  const [loading, setLoading]             = useState(true)
  const [error, setError]                 = useState(null)
  const [filters, setFilters]             = useState({ search: '', status: 'ALL', executionType: 'ALL' })

  // Fetch all executions once; filtering is purely client-side.
  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const data = await executionService.getAll()
      console.log('[Executions Raw Data]', data)
      setAllExecutions(Array.isArray(data) ? data : [])
    }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  // ── Client-side filtering ──────────────────────────────────────────────────
  const filteredExecutions = allExecutions.filter((execution) => {
    const matchesType   = filters.executionType === 'ALL' || execution.normalizedType   === filters.executionType
    const matchesStatus = filters.status        === 'ALL' || execution.normalizedStatus === filters.status
    const q             = filters.search.trim().toLowerCase()
    const matchesSearch = !q ||
      (execution.entityName || '').toLowerCase().includes(q) ||
      String(execution.id ?? '').includes(q)
    return matchesType && matchesStatus && matchesSearch
  })

  console.log('[Executions] total', allExecutions.length, 'filtered', filteredExecutions.length)
  console.log('[Executions Selected Type Filter]',   filters.executionType)
  console.log('[Executions Selected Status Filter]', filters.status)

  // ── Pagination ─────────────────────────────────────────────────────────────
  const {
    currentPage, pageSize, totalItems,
    paginatedItems: paginatedExecutions,
    setCurrentPage, setPageSize,
  } = useClientPagination(filteredExecutions)

  // Reset to page 1 whenever a filter changes
  useEffect(() => {
    setCurrentPage(1)
  }, [filters.search, filters.status, filters.executionType]) // eslint-disable-line react-hooks/exhaustive-deps

  // ── UI helpers ─────────────────────────────────────────────────────────────
  const hasActiveFilters = filters.executionType !== 'ALL' || filters.status !== 'ALL' || !!filters.search.trim()
  const emptyMessage     = hasActiveFilters ? 'No executions found for selected filters.' : 'No executions yet.'

  const subtitleText = hasActiveFilters
    ? `${filteredExecutions.length} of ${allExecutions.length} execution${allExecutions.length !== 1 ? 's' : ''} match`
    : `${allExecutions.length} execution${allExecutions.length !== 1 ? 's' : ''} recorded`

  return (
    <div>
      <PageHeader
        title="Executions"
        subtitle={subtitleText}
        actions={(isAdmin || isTester) && <Button variant="primary" onClick={() => navigate('/executions/run')}>+ Run Execution</Button>}
      />

      <div className="rules-filters">
        <div className="rules-search-wrapper">
          <Input
            name="search"
            placeholder="Search by name…"
            value={filters.search}
            onChange={(e) => setFilters((p) => ({ ...p, search: e.target.value }))}
          />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select
            name="status"
            options={STATUS_OPTIONS}
            value={filters.status}
            onChange={(e) => setFilters((p) => ({ ...p, status: e.target.value }))}
          />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select
            name="executionType"
            options={TYPE_OPTIONS}
            value={filters.executionType}
            onChange={(e) => setFilters((p) => ({ ...p, executionType: e.target.value }))}
          />
        </div>
      </div>

      {error ? (
        <ErrorMessage title="Failed to load executions" message={error} onRetry={load} />
      ) : (
        <>
          <Card noPadding>
            <ExecutionResultTable
              executions={paginatedExecutions}
              loading={loading}
              emptyMessage={emptyMessage}
            />
          </Card>

          {!loading && (
            <Pagination
              currentPage={currentPage}
              pageSize={pageSize}
              totalItems={totalItems}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          )}
        </>
      )}
    </div>
  )
}

export default ExecutionsPage
