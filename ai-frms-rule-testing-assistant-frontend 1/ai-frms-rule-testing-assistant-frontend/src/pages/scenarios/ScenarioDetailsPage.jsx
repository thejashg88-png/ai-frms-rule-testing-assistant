import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import ScenarioDetails from '../../components/scenarios/ScenarioDetails'
import scenarioService from '../../services/scenarioService'

const ScenarioDetailsPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [scenario, setScenario] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setLoading(true)
    scenarioService.getById(id)
      .then(setScenario)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <Loader message="Loading scenario…" />
  if (error) return <ErrorMessage title="Failed to load scenario" message={error} />

  return (
    <div>
      <PageHeader
        title={scenario?.name ?? 'Scenario Details'}
        subtitle={`Type: ${scenario?.scenarioType} · ${scenario?.testCaseCount ?? 0} test cases`}
        actions={
          <div style={{ display: 'flex', gap: 10 }}>
            <Button variant="ghost" onClick={() => navigate('/scenarios')}>← Back</Button>
            <Button variant="outline" onClick={() => navigate(`/scenarios/${id}/edit`)}>Edit</Button>
            <Button variant="primary" onClick={() => navigate(`/executions/run?scenarioId=${id}`)}>Run Scenario</Button>
          </div>
        }
      />
      <Card title="Scenario Information">
        <ScenarioDetails scenario={scenario} />
      </Card>
    </div>
  )
}

export default ScenarioDetailsPage
