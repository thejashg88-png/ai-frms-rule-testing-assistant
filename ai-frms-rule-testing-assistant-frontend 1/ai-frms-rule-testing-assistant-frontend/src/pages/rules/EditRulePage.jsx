import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import { useToast } from '../../hooks/useToast'
import ruleService from '../../services/ruleService'
import RuleForm from './RuleForm'
import '../../styles/pages.css'

const EditRulePage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addToast } = useToast()

  const [rule, setRule] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [retryKey, setRetryKey] = useState(0)

  // retryKey is incremented by the ErrorMessage retry button to re-trigger this effect.
  useEffect(() => {
    setLoading(true)
    setLoadError(null)
    ruleService
      .getById(id)
      .then(setRule)
      .catch((err) => setLoadError(err.message))
      .finally(() => setLoading(false))
  }, [id, retryKey])

  const handleSubmit = async (formData) => {
    setSaving(true)
    try {
      await ruleService.update(id, formData)
      addToast('Rule updated successfully', 'success')
      navigate('/rules')
    } catch (err) {
      addToast(err.message, 'error')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Loader message="Loading rule..." />

  if (loadError) {
    return (
      <ErrorMessage
        title="Failed to load rule"
        message={loadError}
        onRetry={() => setRetryKey((k) => k + 1)}
      />
    )
  }

  return (
    <div>
      <PageHeader
        title="Edit Rule"
        subtitle={`Editing: ${rule?.name}`}
        actions={
          <Button variant="ghost" onClick={() => navigate('/rules')}>
            ← Back to Rules
          </Button>
        }
      />
      <Card title="Rule Details" subtitle="Update the rule configuration">
        <RuleForm
          initialValues={rule}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/rules')}
          loading={saving}
          submitLabel="Update Rule"
        />
      </Card>
    </div>
  )
}

export default EditRulePage
