import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import ExecutionDetails from '../../components/executions/ExecutionDetails'
import executionService from '../../services/executionService'

const ExecutionDetailsPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [execution, setExecution] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setLoading(true)
    executionService.getById(id)
      .then(setExecution)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <Loader message="Loading execution details…" />
  if (error) return <ErrorMessage title="Failed to load execution" message={error} />

  return (
    <div>
      <PageHeader
        title="Execution Details"
        subtitle={`${execution?.executionType} · ${execution?.entityName}`}
        actions={<Button variant="ghost" onClick={() => navigate('/executions')}>← Back</Button>}
      />
      <Card title="Execution Result">
        <ExecutionDetails execution={execution} />
      </Card>
    </div>
  )
}

export default ExecutionDetailsPage
