import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import TextArea from '../common/TextArea'
import Button from '../common/Button'
import { RULE_TYPES } from '../../data/ruleTypes'
import '../../styles/pages.css'

const STATUS_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const EXPECTED_RESULT_OPTIONS = [
  { value: 'ACCEPT',  label: 'Accept' },
  { value: 'MONITOR', label: 'Monitor' },
  { value: 'REJECT',  label: 'Reject' },
]

const EMPTY = { scenarioName: '', description: '', ruleType: '', status: 'ACTIVE', expectedResult: '' }

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
    if (!form.scenarioName.trim())  e.scenarioName  = 'Scenario name is required'
    if (!form.description.trim())   e.description   = 'Description is required'
    if (!form.ruleType)             e.ruleType       = 'Rule type is required'
    if (!form.expectedResult)       e.expectedResult = 'Expected result is required'
    if (!form.status)               e.status         = 'Status is required'
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    onSubmit({
      scenarioName:   form.scenarioName.trim(),
      description:    form.description.trim(),
      ruleType:       form.ruleType,
      status:         form.status,
      expectedResult: form.expectedResult,
    })
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Scenario Information</h3>
        <Input label="Scenario Name" name="scenarioName" placeholder="e.g. High Value Transaction Tests"
          value={form.scenarioName} onChange={set} error={errors.scenarioName} required />
        <TextArea label="Description" name="description" placeholder="Describe the test scenario purpose"
          value={form.description} onChange={set} error={errors.description} rows={3} required />
        <div className="rule-form-grid-3">
          <Select label="Rule Type" name="ruleType" placeholder="Select rule type…"
            options={RULE_TYPES} value={form.ruleType} onChange={set} error={errors.ruleType} required />
          <Select label="Expected Result" name="expectedResult" placeholder="Select expected result…"
            options={EXPECTED_RESULT_OPTIONS} value={form.expectedResult} onChange={set} error={errors.expectedResult} required />
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
