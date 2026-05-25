import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import ScenarioForm from '../../components/scenarios/ScenarioForm'
import { useToast } from '../../hooks/useToast'
import scenarioService from '../../services/scenarioService'

const CreateScenarioPage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (formData) => {
    setLoading(true)
    try {
      await scenarioService.create(formData)
      addToast('Scenario created successfully', 'success')
      navigate('/scenarios')
    } catch (err) { addToast(err.message, 'error') }
    finally { setLoading(false) }
  }

  return (
    <div>
      <PageHeader title="Create Scenario" subtitle="Define a new test scenario"
        actions={<Button variant="ghost" onClick={() => navigate('/scenarios')}>← Back</Button>} />
      <Card title="Scenario Details">
        <ScenarioForm onSubmit={handleSubmit} onCancel={() => navigate('/scenarios')} loading={loading} submitLabel="Create Scenario" />
      </Card>
    </div>
  )
}

export default CreateScenarioPage
