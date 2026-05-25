import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import TextArea from '../common/TextArea'
import Button from '../common/Button'
import '../../styles/pages.css'

const SCENARIO_TYPE_OPTIONS = [
  { value: 'SINGLE', label: 'Single Rule' },
  { value: 'BULK',   label: 'Multi-Rule (Bulk)' },
]

const STATUS_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const EMPTY = { name: '', description: '', scenarioType: 'SINGLE', status: 'ACTIVE' }

const ScenarioForm = ({ initialValues = {}, onSubmit, onCancel, loading = false, submitLabel = 'Save' }) => {
  const [form, setForm] = useState({ ...EMPTY, ...initialValues })
  const [errors, setErrors] = useState({})

  const set = (e) => {
    const { name, value } = e.target
    setForm((p) => ({ ...p, [name]: value }))
    if (errors[name]) setErrors((p) => ({ ...p, [name]: '' }))
  }

  const validate = () => {
    const e = {}
    if (!form.name.trim()) e.name = 'Scenario name is required'
    if (!form.scenarioType) e.scenarioType = 'Scenario type is required'
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    onSubmit({ name: form.name.trim(), description: form.description.trim(), scenarioType: form.scenarioType, status: form.status })
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Scenario Information</h3>
        <Input label="Scenario Name" name="name" placeholder="e.g. High Value Transaction Tests"
          value={form.name} onChange={set} error={errors.name} required />
        <TextArea label="Description" name="description" placeholder="Describe the test scenario purpose"
          value={form.description} onChange={set} rows={3} />
        <div className="rule-form-grid-3">
          <Select label="Scenario Type" name="scenarioType" options={SCENARIO_TYPE_OPTIONS}
            value={form.scenarioType} onChange={set} error={errors.scenarioType} required />
          <Select label="Status" name="status" options={STATUS_OPTIONS}
            value={form.status} onChange={set} />
        </div>
      </div>
      <div className="rule-form-actions">
        <Button type="submit" variant="primary" loading={loading}>{submitLabel}</Button>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={loading}>Cancel</Button>
      </div>
    </form>
  )
}

export default ScenarioForm
