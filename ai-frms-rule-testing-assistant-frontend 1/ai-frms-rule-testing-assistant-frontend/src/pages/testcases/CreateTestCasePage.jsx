import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import TestCaseForm from '../../components/testcases/TestCaseForm'
import { useToast } from '../../hooks/useToast'
import testCaseService from '../../services/testCaseService'

const CreateTestCasePage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (formData) => {
    setLoading(true)
    try {
      await testCaseService.create(formData)
      addToast('Test case created successfully', 'success')
      navigate('/testcases')
    } catch (err) { addToast(err.message, 'error') }
    finally { setLoading(false) }
  }

  return (
    <div>
      <PageHeader title="Create Test Case" subtitle="Define a new test case for rule validation"
        actions={<Button variant="ghost" onClick={() => navigate('/testcases')}>← Back</Button>} />
      <Card title="Test Case Details">
        <TestCaseForm onSubmit={handleSubmit} onCancel={() => navigate('/testcases')} loading={loading} submitLabel="Create Test Case" />
      </Card>
    </div>
  )
}

export default CreateTestCasePage
