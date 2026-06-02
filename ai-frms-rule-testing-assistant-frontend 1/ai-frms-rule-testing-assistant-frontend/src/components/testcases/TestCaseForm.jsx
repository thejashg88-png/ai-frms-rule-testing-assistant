import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import Button from '../common/Button'
import { RULE_TYPES } from '../../data/ruleTypes'
import {
  ALL_TEST_CASE_FIELDS,
  getRuleFieldConfig,
  shouldShowTestCaseField,
} from '../../data/ruleFieldConfig'
import '../../styles/pages.css'

const EXPECTED_OUTCOME_OPTIONS = [
  { value: 'PASS', label: 'Pass (rule behaves as expected)' },
  { value: 'FAIL', label: 'Fail (rule should trigger rejection)' },
]

const EXPECTED_ACTION_OPTIONS = [
  { value: 'ACCEPT',  label: 'Accept' },
  { value: 'MONITOR', label: 'Monitor' },
  { value: 'REJECT',  label: 'Reject' },
]

const RISK_LEVEL_OPTIONS = [
  { value: 'LOW',    label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH',   label: 'High' },
]

const TXN_TYPE_OPTIONS = [
  { value: 'PURCHASE',   label: 'Purchase' },
  { value: 'REFUND',     label: 'Refund' },
  { value: 'WITHDRAWAL', label: 'Withdrawal' },
  { value: 'TRANSFER',   label: 'Transfer' },
]

const CHANNEL_OPTIONS = [
  { value: 'ONLINE', label: 'Online' },
  { value: 'POS',    label: 'POS' },
  { value: 'ATM',    label: 'ATM' },
  { value: 'MOBILE', label: 'Mobile' },
]

const CURRENCY_OPTIONS = [
  { value: 'INR', label: 'INR (₹)' },
  { value: 'USD', label: 'USD ($)' },
  { value: 'EUR', label: 'EUR (€)' },
  { value: 'GBP', label: 'GBP (£)' },
  { value: 'SGD', label: 'SGD' },
]

const STATUS_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

// Blank option prepended to RULE_TYPES for the ruleType Select
const RULE_TYPE_OPTIONS = [{ value: '', label: '— None (show all fields) —' }, ...RULE_TYPES]

const EMPTY = {
  name: '', description: '',
  scenarioId: '', scenarioName: '',
  ruleId: '', ruleName: '', ruleType: '',
  expectedResult: 'PASS', expectedAction: 'REJECT', expectedRiskLevel: 'MEDIUM',
  status: 'ACTIVE',
  inputCardNumber: '', inputAmount: '', inputMerchantId: '',
  inputTransactionType: 'PURCHASE', inputChannel: 'ONLINE',
  inputCountry: '', inputCurrency: 'INR',
  inputTransactionTime: '', inputMccCode: '',
}

const strVal = (v) => (v == null ? '' : String(v))

const TestCaseForm = ({ initialValues = {}, onSubmit, onCancel, loading = false, submitLabel = 'Save' }) => {
  const init      = initialValues
  const inputData = init.inputData ?? {}

  const [form, setForm] = useState({
    ...EMPTY,
    ...init,
    scenarioId:           strVal(init.scenarioId),
    ruleId:               strVal(init.ruleId),
    ruleType:             strVal(init.ruleType ?? init.expectedRuleType),
    inputCardNumber:      inputData.cardNumber      ?? '',
    inputAmount:          inputData.amount != null ? String(inputData.amount) : '',
    inputMerchantId:      inputData.merchantId      ?? '',
    inputTransactionType: inputData.transactionType ?? 'PURCHASE',
    inputChannel:         inputData.channel         ?? 'ONLINE',
    inputCountry:         inputData.country         ?? inputData.countryCode ?? '',
    inputCurrency:        inputData.currency        ?? 'INR',
    inputTransactionTime: inputData.transactionTime ?? '',
    inputMccCode:         inputData.mccCode         ?? '',
  })
  const [errors, setErrors] = useState({})

  const set = (e) => {
    const { name, value } = e.target
    setForm((p) => ({ ...p, [name]: value }))
    if (errors[name]) setErrors((p) => ({ ...p, [name]: '' }))
  }

  // Only fields applicable to the selected ruleType are shown AND included in the payload.
  const show = (field) => shouldShowTestCaseField(form.ruleType, field)

  const validate = () => {
    const e = {}
    if (!form.name.trim())                            e.name              = 'Name is required'
    if (!form.scenarioId || !Number(form.scenarioId)) e.scenarioId        = 'Scenario ID is required'
    if (!form.ruleId     || !Number(form.ruleId))     e.ruleId            = 'Rule ID is required'
    if (!form.expectedResult)                         e.expectedResult    = 'Expected outcome is required'
    if (!form.expectedAction)                         e.expectedAction    = 'Expected action is required'
    if (!form.expectedRiskLevel)                      e.expectedRiskLevel = 'Risk level is required'
    // Only validate transaction fields that are visible for this rule type
    if (show('cardNumber') && !form.inputCardNumber.trim())
      e.inputCardNumber = 'Card number is required'
    if (show('amount') && (!form.inputAmount || Number(form.inputAmount) <= 0))
      e.inputAmount = 'Amount must be > 0'
    if (show('countryCode') && !form.inputCountry.trim())
      e.inputCountry = 'Country code is required'
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }

    // Only include visible fields in inputData so hidden fields are not sent as empty strings.
    // The rule engine may misinterpret empty string values as zero or invalid input.
    const inputDataPayload = {
      ...(show('cardNumber')      && { cardNumber:       form.inputCardNumber.trim() }),
      ...(show('amount')          && { amount:           Number(form.inputAmount) }),
      ...(show('merchantId') && form.inputMerchantId.trim() && { merchantId: form.inputMerchantId.trim() }),
      ...(show('transactionType') && { transactionType:  form.inputTransactionType }),
      ...(show('channel')         && { channel:          form.inputChannel }),
      ...(show('countryCode')     && { country:          form.inputCountry.trim().toUpperCase() }),
      ...(show('currency')        && { currency:         form.inputCurrency || 'INR' }),
      ...(show('transactionTime') && form.inputTransactionTime && { transactionTime: form.inputTransactionTime }),
      ...(show('mccCode') && form.inputMccCode.trim() && { mccCode: form.inputMccCode.trim() }),
    }

    const payload = {
      name:              form.name.trim(),
      description:       form.description.trim(),
      scenarioId:        Number(form.scenarioId),
      scenarioName:      form.scenarioName.trim(),
      ruleId:            Number(form.ruleId),
      ruleName:          form.ruleName.trim(),
      ruleType:          form.ruleType.trim(),
      expectedResult:    form.expectedResult,
      expectedAction:    form.expectedAction,
      expectedRiskLevel: form.expectedRiskLevel,
      status:            form.status,
      inputData:         inputDataPayload,
    }

    console.log('[TestCaseForm] selected ruleType', form.ruleType)
    console.log('[TestCaseForm] visible test case fields', ALL_TEST_CASE_FIELDS.filter((f) => show(f)))
    console.log('[Test Case Payload]', payload)
    onSubmit(payload)
  }

  const config = getRuleFieldConfig(form.ruleType)

  return (
    <form onSubmit={handleSubmit} noValidate>
      {/* ── Basic Information ── */}
      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Basic Information</h3>
        <Input
          label="Test Case Name" name="name"
          placeholder="e.g. Credit limit exceeded — should REJECT"
          value={form.name} onChange={set} error={errors.name} required
        />
        <Input
          label="Description" name="description"
          placeholder="What this test case validates"
          value={form.description} onChange={set}
        />
        <div className="rule-form-grid-3">
          <Input
            label="Scenario ID" name="scenarioId" type="number" placeholder="e.g. 1"
            value={form.scenarioId} onChange={set} error={errors.scenarioId} required
          />
          <Input
            label="Scenario Name" name="scenarioName" placeholder="Scenario name"
            value={form.scenarioName} onChange={set}
          />
          <Select label="Status" name="status" options={STATUS_OPTIONS} value={form.status} onChange={set} />
        </div>
        <div className="rule-form-grid-3">
          <Input
            label="Rule ID" name="ruleId" type="number" placeholder="e.g. 1"
            value={form.ruleId} onChange={set} error={errors.ruleId} required
          />
          <Input
            label="Rule Name" name="ruleName" placeholder="Rule name"
            value={form.ruleName} onChange={set}
          />
          <Select
            label="Rule Type" name="ruleType"
            placeholder="None (show all fields)"
            options={RULE_TYPE_OPTIONS}
            value={form.ruleType} onChange={set}
          />
        </div>
        {config && (
          <p style={{ margin: '8px 0 0', fontSize: 13, color: 'var(--primary)', lineHeight: 1.5 }}>
            {config.hint}
          </p>
        )}
      </div>

      {/* ── Transaction Input Data ── */}
      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Transaction Input Data</h3>
        {form.ruleType
          ? <p className="rule-form-hint">Showing fields required for the selected rule type.</p>
          : <p className="rule-form-hint">Select a rule type above to filter the required transaction fields.</p>
        }
        <div className="rule-form-grid-3">
          {show('cardNumber') && (
            <Input
              label="Card Number" name="inputCardNumber" placeholder="16-digit card number"
              value={form.inputCardNumber} onChange={set} error={errors.inputCardNumber} required
            />
          )}
          {show('amount') && (
            <Input
              label="Amount" name="inputAmount" type="number" placeholder="e.g. 150000"
              value={form.inputAmount} onChange={set} error={errors.inputAmount} required
            />
          )}
          {show('currency') && (
            <Select
              label="Currency" name="inputCurrency"
              options={CURRENCY_OPTIONS}
              value={form.inputCurrency} onChange={set}
            />
          )}
          {show('merchantId') && (
            <Input
              label="Merchant ID" name="inputMerchantId" placeholder="e.g. MID00001"
              value={form.inputMerchantId} onChange={set}
            />
          )}
          {show('transactionType') && (
            <Select
              label="Transaction Type" name="inputTransactionType"
              options={TXN_TYPE_OPTIONS}
              value={form.inputTransactionType} onChange={set}
            />
          )}
          {show('channel') && (
            <Select
              label="Channel" name="inputChannel"
              options={CHANNEL_OPTIONS}
              value={form.inputChannel} onChange={set}
            />
          )}
          {show('countryCode') && (
            <Input
              label="Country Code" name="inputCountry" placeholder="e.g. IN"
              value={form.inputCountry} onChange={set} error={errors.inputCountry} required
            />
          )}
          {show('transactionTime') && (
            <Input
              label="Transaction Time" name="inputTransactionTime" type="time"
              helperText="Abnormal hour e.g. 02:00 triggers the ABNORMAL_HOUR rule"
              value={form.inputTransactionTime} onChange={set}
            />
          )}
          {show('mccCode') && (
            <Input
              label="MCC Code" name="inputMccCode" placeholder="e.g. 5411"
              helperText="Merchant Category Code used for inconsistency detection"
              value={form.inputMccCode} onChange={set}
            />
          )}
        </div>
      </div>

      {/* ── Expected Outcome ── */}
      <div className="rule-form-section rule-form-section-last">
        <h3 className="rule-form-section-title">Expected Outcome</h3>
        <div className="rule-form-grid-3">
          <Select
            label="Expected Outcome" name="expectedResult"
            options={EXPECTED_OUTCOME_OPTIONS}
            value={form.expectedResult} onChange={set}
            error={errors.expectedResult} required
          />
          <Select
            label="Expected Action" name="expectedAction"
            options={EXPECTED_ACTION_OPTIONS}
            value={form.expectedAction} onChange={set}
            error={errors.expectedAction} required
          />
          <Select
            label="Expected Risk Level" name="expectedRiskLevel"
            options={RISK_LEVEL_OPTIONS}
            value={form.expectedRiskLevel} onChange={set}
            error={errors.expectedRiskLevel} required
          />
        </div>
      </div>

      <div className="rule-form-actions">
        <Button type="submit" variant="primary" loading={loading}>{submitLabel}</Button>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={loading}>Cancel</Button>
      </div>
    </form>
  )
}

export default TestCaseForm
