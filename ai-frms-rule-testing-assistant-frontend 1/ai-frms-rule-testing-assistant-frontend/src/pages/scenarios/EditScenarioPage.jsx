import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import ScenarioForm from '../../components/scenarios/ScenarioForm'
import { useToast } from '../../hooks/useToast'
import scenarioService from '../../services/scenarioService'

const EditScenarioPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addToast } = useToast()
  const [scenario, setScenario] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setLoading(true)
    scenarioService.getById(id)
      .then(setScenario)
      .catch((err) => setLoadError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  const handleSubmit = async (formData) => {
    setSaving(true)
    try {
      await scenarioService.update(id, formData)
      addToast('Scenario updated', 'success')
      navigate('/scenarios')
    } catch (err) { addToast(err.message, 'error') }
    finally { setSaving(false) }
  }

  if (loading) return <Loader message="Loading scenario…" />
  if (loadError) return <ErrorMessage title="Failed to load scenario" message={loadError} />

  return (
    <div>
      <PageHeader title="Edit Scenario" subtitle={`Editing: ${scenario?.name}`}
        actions={<Button variant="ghost" onClick={() => navigate('/scenarios')}>← Back</Button>} />
      <Card title="Scenario Details">
        <ScenarioForm initialValues={scenario} onSubmit={handleSubmit} onCancel={() => navigate('/scenarios')} loading={saving} submitLabel="Update Scenario" />
      </Card>
    </div>
  )
}

export default EditScenarioPage
