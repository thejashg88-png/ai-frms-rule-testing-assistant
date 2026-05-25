import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import ErrorMessage from '../../components/common/ErrorMessage'
import ScenarioTable from '../../components/scenarios/ScenarioTable'
import ScenarioFilter from '../../components/scenarios/ScenarioFilter'
import { useToast } from '../../hooks/useToast'
import scenarioService from '../../services/scenarioService'
import '../../styles/pages.css'

const ScenariosPage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()

  const [scenarios, setScenarios] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [confirmId, setConfirmId] = useState(null)
  const [filters, setFilters] = useState({ search: '', status: '', scenarioType: '' })

  const loadScenarios = useCallback(async () => {
    setLoading(true); setError(null)
    try { setScenarios(await scenarioService.getAll(filters)) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }, [filters])

  useEffect(() => { loadScenarios() }, [loadScenarios])

  const handleDelete = async (id) => {
    setDeleting(id)
    try {
      await scenarioService.delete(id)
      setScenarios((prev) => prev.filter((s) => s.id !== id))
      addToast('Scenario deleted', 'success')
    } catch (err) { addToast(err.message, 'error') }
    finally { setDeleting(null); setConfirmId(null) }
  }

  return (
    <div>
      <PageHeader
        title="Scenarios"
        subtitle={`${scenarios.length} test scenario${scenarios.length !== 1 ? 's' : ''}`}
        actions={<Button variant="primary" onClick={() => navigate('/scenarios/create')}>+ Create Scenario</Button>}
      />
      <ScenarioFilter
        filters={filters}
        onChange={(e) => setFilters((p) => ({ ...p, [e.target.name]: e.target.value }))}
      />
      {error ? (
        <ErrorMessage title="Failed to load scenarios" message={error} onRetry={loadScenarios} />
      ) : (
        <Card noPadding>
          <ScenarioTable
            scenarios={scenarios}
            loading={loading}
            onDelete={(id) => setConfirmId(id)}
            deletingId={deleting}
            confirmId={confirmId}
            onConfirmDelete={handleDelete}
            onCancelDelete={() => setConfirmId(null)}
          />
        </Card>
      )}
    </div>
  )
}

export default ScenariosPage
