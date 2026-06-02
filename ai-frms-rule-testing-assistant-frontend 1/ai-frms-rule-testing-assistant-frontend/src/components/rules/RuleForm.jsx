import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import Button from '../common/Button'
import { RULE_TYPES } from '../../data/ruleTypes'
import {
  ALL_RULE_FIELDS,
  getRuleFieldConfig,
  shouldShowRuleField,
  isRuleFieldRequired,
} from '../../data/ruleFieldConfig'
import '../../styles/pages.css'

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
    if (name === 'ruleType') {
      // Clear parameter fields not applicable to the newly selected rule type
      const cleared = {}
      ALL_RULE_FIELDS.forEach((f) => {
        if (!shouldShowRuleField(value, f)) cleared[f] = ''
      })
      setForm((p) => ({ ...p, ruleType: value, ...cleared }))
      setErrors((p) => ({ ...p, ruleType: '' }))
    } else {
      setForm((p) => ({ ...p, [name]: value }))
      if (errors[name]) setErrors((p) => ({ ...p, [name]: '' }))
    }
  }

  const show = (field) => shouldShowRuleField(form.ruleType, field)

  const validate = () => {
    const e = {}
    if (!form.name.trim())  e.name     = 'Rule name is required'
    if (!form.ruleType)     e.ruleType = 'Rule type is required'
    if (!form.action)       e.action   = 'Action is required'
    if (!form.status)       e.status   = 'Status is required'
    ALL_RULE_FIELDS.forEach((f) => {
      if (!show(f)) return                                   // skip hidden fields
      if (isRuleFieldRequired(form.ruleType, f) && (form[f] === '' || form[f] == null)) {
        e[f] = 'Required for this rule type'
      } else if (form[f] !== '' && Number(form[f]) < 0) {
        e[f] = 'Must be 0 or greater'
      }
    })
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    const payload = {
      name:                form.name.trim(),
      description:         form.description.trim(),
      ruleType:            form.ruleType,
      action:              form.action,
      status:              form.status,
      // Send null for fields hidden by the selected rule type
      txnCount:            show('txnCount')            ? numOrNull(form.txnCount)            : null,
      maxAmount:           show('maxAmount')           ? numOrNull(form.maxAmount)           : null,
      txnAmount:           show('txnAmount')           ? numOrNull(form.txnAmount)           : null,
      frequency:           show('frequency')           ? numOrNull(form.frequency)           : null,
      percentageThreshold: show('percentageThreshold') ? numOrNull(form.percentageThreshold) : null,
    }
    console.log('[RuleForm] selected ruleType', form.ruleType)
    console.log('[RuleForm] visible rule fields', ALL_RULE_FIELDS.filter((f) => show(f)))
    console.log('[Rule Payload]', payload)
    onSubmit(payload)
  }

  const config = getRuleFieldConfig(form.ruleType)

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
            options={RULE_TYPES} value={form.ruleType} onChange={set} error={errors.ruleType} required />
          <Select label="Action" name="action" placeholder="Select action"
            options={ACTION_OPTIONS} value={form.action} onChange={set} error={errors.action} required />
          <Select label="Status" name="status" placeholder="Select status"
            options={STATUS_OPTIONS} value={form.status} onChange={set} error={errors.status} required />
        </div>
        {config && (
          <p style={{ margin: '8px 0 0', fontSize: 13, color: 'var(--primary)', lineHeight: 1.5 }}>
            {config.hint}
          </p>
        )}
      </div>

      <div className="rule-form-section rule-form-section-last">
        <h3 className="rule-form-section-title">Thresholds &amp; Parameters</h3>
        {form.ruleType
          ? <p className="rule-form-hint">Showing only the parameters required for the selected rule type.</p>
          : <p className="rule-form-hint">Select a rule type above to filter the required parameters.</p>
        }
        <div className="rule-form-grid-3">
          {show('txnCount') && (
            <Input label="Transaction Count" name="txnCount" type="number" placeholder="e.g. 5"
              value={form.txnCount} onChange={set} error={errors.txnCount}
              required={isRuleFieldRequired(form.ruleType, 'txnCount')} />
          )}
          {show('maxAmount') && (
            <Input label="Max Amount" name="maxAmount" type="number" placeholder="e.g. 10000"
              value={form.maxAmount} onChange={set} error={errors.maxAmount}
              required={isRuleFieldRequired(form.ruleType, 'maxAmount')} />
          )}
          {show('txnAmount') && (
            <Input label="Transaction Amount" name="txnAmount" type="number" placeholder="e.g. 500"
              value={form.txnAmount} onChange={set} error={errors.txnAmount}
              required={isRuleFieldRequired(form.ruleType, 'txnAmount')} />
          )}
          {show('frequency') && (
            <Input label="Frequency" name="frequency" type="number" placeholder="e.g. 3"
              value={form.frequency} onChange={set} error={errors.frequency}
              required={isRuleFieldRequired(form.ruleType, 'frequency')} />
          )}
          {show('percentageThreshold') && (
            <Input label="Percentage Threshold" name="percentageThreshold" type="number" placeholder="e.g. 80"
              value={form.percentageThreshold} onChange={set} error={errors.percentageThreshold}
              required={isRuleFieldRequired(form.ruleType, 'percentageThreshold')} />
          )}
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
