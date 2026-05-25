import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import TestCaseForm from '../../components/testcases/TestCaseForm'
import { useToast } from '../../hooks/useToast'
import testCaseService from '../../services/testCaseService'

const EditTestCasePage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addToast } = useToast()
  const [testCase, setTestCase] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setLoading(true)
    testCaseService.getById(id)
      .then(setTestCase)
      .catch((err) => setLoadError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  const handleSubmit = async (formData) => {
    setSaving(true)
    try {
      await testCaseService.update(id, formData)
      addToast('Test case updated', 'success')
      navigate('/testcases')
    } catch (err) { addToast(err.message, 'error') }
    finally { setSaving(false) }
  }

  if (loading) return <Loader message="Loading test case…" />
  if (loadError) return <ErrorMessage title="Failed to load test case" message={loadError} />

  return (
    <div>
      <PageHeader title="Edit Test Case" subtitle={`Editing: ${testCase?.name}`}
        actions={<Button variant="ghost" onClick={() => navigate('/testcases')}>← Back</Button>} />
      <Card title="Test Case Details">
        <TestCaseForm initialValues={testCase} onSubmit={handleSubmit} onCancel={() => navigate('/testcases')} loading={saving} submitLabel="Update Test Case" />
      </Card>
    </div>
  )
}

export default EditTestCasePage
