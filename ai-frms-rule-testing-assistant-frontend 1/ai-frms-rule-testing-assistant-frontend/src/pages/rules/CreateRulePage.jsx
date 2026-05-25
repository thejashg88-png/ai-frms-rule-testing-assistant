import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import { useToast } from '../../hooks/useToast'
import ruleService from '../../services/ruleService'
import RuleForm from './RuleForm'
import '../../styles/pages.css'

const CreateRulePage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (formData) => {
    setLoading(true)
    try {
      await ruleService.create(formData)
      addToast('Rule created successfully', 'success')
      navigate('/rules')
    } catch (err) {
      addToast(err.message, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Create Rule"
        subtitle="Define a new FRMS business rule"
        actions={
          <Button variant="ghost" onClick={() => navigate('/rules')}>
            ← Back to Rules
          </Button>
        }
      />
      <Card title="Rule Details" subtitle="Fill in the rule configuration below">
        <RuleForm
          onSubmit={handleSubmit}
          onCancel={() => navigate('/rules')}
          loading={loading}
          submitLabel="Create Rule"
        />
      </Card>
    </div>
  )
}

export default CreateRulePage
