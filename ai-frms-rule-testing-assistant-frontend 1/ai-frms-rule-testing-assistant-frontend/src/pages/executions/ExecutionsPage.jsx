import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import Select from '../../components/common/Select'
import Card from '../../components/common/Card'
import ErrorMessage from '../../components/common/ErrorMessage'
import ExecutionResultTable from '../../components/executions/ExecutionResultTable'
import { useToast } from '../../hooks/useToast'
import executionService from '../../services/executionService'
import '../../styles/pages.css'

const STATUS_OPTIONS = [
  { value: 'PASSED', label: 'Passed' },
  { value: 'FAILED', label: 'Failed' },
]

const TYPE_OPTIONS = [
  { value: 'TEST_CASE', label: 'Test Case' },
  { value: 'SCENARIO',  label: 'Scenario' },
]

const ExecutionsPage = () => {
  const navigate = useNavigate()

  const [executions, setExecutions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filters, setFilters] = useState({ search: '', status: '', executionType: '' })

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try { setExecutions(await executionService.getAll(filters)) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }, [filters])

  useEffect(() => { load() }, [load])

  return (
    <div>
      <PageHeader
        title="Executions"
        subtitle={`${executions.length} execution${executions.length !== 1 ? 's' : ''} recorded`}
        actions={<Button variant="primary" onClick={() => navigate('/executions/run')}>+ Run Execution</Button>}
      />

      <div className="rules-filters">
        <div className="rules-search-wrapper">
          <Input name="search" placeholder="Search by name…" value={filters.search}
            onChange={(e) => setFilters((p) => ({ ...p, search: e.target.value }))} />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select name="status" placeholder="All Statuses" options={STATUS_OPTIONS} value={filters.status}
            onChange={(e) => setFilters((p) => ({ ...p, status: e.target.value }))} />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select name="executionType" placeholder="All Types" options={TYPE_OPTIONS} value={filters.executionType}
            onChange={(e) => setFilters((p) => ({ ...p, executionType: e.target.value }))} />
        </div>
      </div>

      {error ? (
        <ErrorMessage title="Failed to load executions" message={error} onRetry={load} />
      ) : (
        <Card noPadding>
          <ExecutionResultTable executions={executions} loading={loading} />
        </Card>
      )}
    </div>
  )
}

export default ExecutionsPage
