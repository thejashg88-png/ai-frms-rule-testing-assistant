import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import ErrorMessage from '../../components/common/ErrorMessage'
import Pagination from '../../components/common/Pagination'
import ScenarioTable from '../../components/scenarios/ScenarioTable'
import ScenarioFilter from '../../components/scenarios/ScenarioFilter'
import { useToast } from '../../hooks/useToast'
import { useAuth } from '../../hooks/useAuth'
import useClientPagination from '../../hooks/useClientPagination'
import scenarioService from '../../services/scenarioService'
import '../../styles/pages.css'

const ScenariosPage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()
  const { isAdmin } = useAuth()

  // All records loaded once from backend
  const [allScenarios, setAllScenarios] = useState([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState(null)
  const [deleting, setDeleting]         = useState(null)
  const [confirmId, setConfirmId]       = useState(null)

  // Client-side filter state (matches ScenarioFilter field names)
  const [filters, setFilters] = useState({ search: '', status: '', scenarioType: '' })

  // Load all scenarios once on mount — no filter params
  const loadScenarios = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const data = await scenarioService.getAll()
      setAllScenarios(Array.isArray(data) ? data : [])
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { loadScenarios() }, [loadScenarios])

  // ── Client-side filtering ──────────────────────────────────────────────────
  const filteredScenarios = allScenarios.filter((s) => {
    if (filters.status       && s.status       !== filters.status)       return false
    if (filters.scenarioType && s.scenarioType !== filters.scenarioType) return false
    if (filters.search) {
      const q = filters.search.toLowerCase()
      if (
        !(s.name        || '').toLowerCase().includes(q) &&
        !(s.description || '').toLowerCase().includes(q)
      ) return false
    }
    return true
  })

  console.log('[Scenarios] total', allScenarios.length, 'filtered', filteredScenarios.length)

  // ── Pagination ─────────────────────────────────────────────────────────────
  const {
    currentPage, pageSize, totalItems,
    paginatedItems: paginatedScenarios,
    setCurrentPage, setPageSize,
  } = useClientPagination(filteredScenarios)

  // Reset to page 1 whenever a filter changes
  useEffect(() => {
    setCurrentPage(1)
  }, [filters.search, filters.status, filters.scenarioType]) // eslint-disable-line react-hooks/exhaustive-deps

  // ── Handlers ───────────────────────────────────────────────────────────────
  const handleFilterChange = (e) =>
    setFilters((p) => ({ ...p, [e.target.name]: e.target.value }))

  const handleDelete = async (id) => {
    setDeleting(id)
    try {
      await scenarioService.delete(id)
      setAllScenarios((prev) => prev.filter((s) => s.id !== id))
      addToast('Scenario deleted', 'success')
    } catch (err) { addToast(err.message, 'error') }
    finally { setDeleting(null); setConfirmId(null) }
  }

  // ── Subtitle ───────────────────────────────────────────────────────────────
  const hasFilter    = filters.search || filters.status || filters.scenarioType
  const subtitleText = hasFilter
    ? `${filteredScenarios.length} of ${allScenarios.length} scenario${allScenarios.length !== 1 ? 's' : ''} match`
    : `${allScenarios.length} test scenario${allScenarios.length !== 1 ? 's' : ''}`

  return (
    <div>
      <PageHeader
        title="Scenarios"
        subtitle={subtitleText}
        actions={isAdmin && <Button variant="primary" onClick={() => navigate('/scenarios/create')}>+ Create Scenario</Button>}
      />

      <ScenarioFilter filters={filters} onChange={handleFilterChange} />

      {error ? (
        <ErrorMessage title="Failed to load scenarios" message={error} onRetry={loadScenarios} />
      ) : (
        <>
          <Card noPadding>
            <ScenarioTable
              scenarios={paginatedScenarios}
              loading={loading}
              onDelete={(id) => setConfirmId(id)}
              deletingId={deleting}
              confirmId={confirmId}
              onConfirmDelete={handleDelete}
              onCancelDelete={() => setConfirmId(null)}
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

export default ScenariosPage
