import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import Button from '../common/Button'
import '../../styles/pages.css'

const RULE_TYPE_OPTIONS = [
  { value: 'CREDIT',      label: 'Credit' },
  { value: 'AMOUNT',      label: 'Amount' },
  { value: 'VELOCITY',    label: 'Velocity' },
  { value: 'GEO',         label: 'Geographic' },
  { value: 'FRAUD',       label: 'Fraud Detection' },
  { value: 'CARD',        label: 'Card' },
  { value: 'TRANSACTION', label: 'Transaction' },
  { value: 'FREQUENCY',   label: 'Frequency' },
]

const ACTION_OPTIONS = [
  { value: 'ACCEPT',  label: 'Accept' },
  { value: 'MONITOR', label: 'Monitor' },
  { value: 'REJECT',  label: 'Reject' },
]

const STATUS_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const EMPTY = {
  name: '', description: '', ruleType: '', action: '', status: 'ACTIVE',
  txnCount: '', maxAmount: '', txnAmount: '', frequency: '', percentageThreshold: '',
}

const numOrNull = (v) => (v === '' || v === null || v === undefined ? null : Number(v))
const strVal   = (v) => (v === null || v === undefined ? '' : String(v))

const RuleForm = ({
  initialValues = {},
  onSubmit,
  onCancel,
  loading = false,
  submitLabel = 'Save Rule',
}) => {
  const [form, setForm] = useState({
    ...EMPTY,
    ...initialValues,
    txnCount:            strVal(initialValues.txnCount),
    maxAmount:           strVal(initialValues.maxAmount),
    txnAmount:           strVal(initialValues.txnAmount),
    frequency:           strVal(initialValues.frequency),
    percentageThreshold: strVal(initialValues.percentageThreshold),
  })
  const [errors, setErrors] = useState({})

  const set = (e) => {
    const { name, value } = e.target
    setForm((p) => ({ ...p, [name]: value }))
    if (errors[name]) setErrors((p) => ({ ...p, [name]: '' }))
  }

  const validate = () => {
    const e = {}
    if (!form.name.trim())  e.name     = 'Rule name is required'
    if (!form.ruleType)     e.ruleType = 'Rule type is required'
    if (!form.action)       e.action   = 'Action is required'
    if (!form.status)       e.status   = 'Status is required'
    ;['txnCount', 'maxAmount', 'txnAmount', 'frequency', 'percentageThreshold'].forEach((f) => {
      if (form[f] !== '' && Number(form[f]) < 0) e[f] = 'Must be 0 or greater'
    })
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    onSubmit({
      name:                form.name.trim(),
      description:         form.description.trim(),
      ruleType:            form.ruleType,
      action:              form.action,
      status:              form.status,
      txnCount:            numOrNull(form.txnCount),
      maxAmount:           numOrNull(form.maxAmount),
      txnAmount:           numOrNull(form.txnAmount),
      frequency:           numOrNull(form.frequency),
      percentageThreshold: numOrNull(form.percentageThreshold),
    })
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Basic Information</h3>
        <Input label="Rule Name" name="name" placeholder="e.g. Credit Limit Check"
          value={form.name} onChange={set} error={errors.name} required />
        <Input label="Description" name="description" placeholder="Describe what this rule validates"
          value={form.description} onChange={set} />
        <div className="rule-form-grid-3">
          <Select label="Rule Type" name="ruleType" placeholder="Select type"
            options={RULE_TYPE_OPTIONS} value={form.ruleType} onChange={set} error={errors.ruleType} required />
          <Select label="Action" name="action" placeholder="Select action"
            options={ACTION_OPTIONS} value={form.action} onChange={set} error={errors.action} required />
          <Select label="Status" name="status" placeholder="Select status"
            options={STATUS_OPTIONS} value={form.status} onChange={set} error={errors.status} required />
        </div>
      </div>

      <div className="rule-form-section rule-form-section-last">
        <h3 className="rule-form-section-title">Thresholds &amp; Parameters</h3>
        <p className="rule-form-hint">Leave blank for fields not applicable to this rule type.</p>
        <div className="rule-form-grid-3">
          <Input label="Transaction Count" name="txnCount" type="number" placeholder="e.g. 5"
            value={form.txnCount} onChange={set} error={errors.txnCount} />
          <Input label="Max Amount" name="maxAmount" type="number" placeholder="e.g. 10000"
            value={form.maxAmount} onChange={set} error={errors.maxAmount} />
          <Input label="Transaction Amount" name="txnAmount" type="number" placeholder="e.g. 500"
            value={form.txnAmount} onChange={set} error={errors.txnAmount} />
          <Input label="Frequency" name="frequency" type="number" placeholder="e.g. 3"
            value={form.frequency} onChange={set} error={errors.frequency} />
          <Input label="Percentage Threshold" name="percentageThreshold" type="number" placeholder="e.g. 80"
            value={form.percentageThreshold} onChange={set} error={errors.percentageThreshold} />
        </div>
      </div>

      <div className="rule-form-actions">
        <Button type="submit" variant="primary" loading={loading} disabled={loading}>
          {submitLabel}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
      </div>
    </form>
  )
}

export default RuleForm
