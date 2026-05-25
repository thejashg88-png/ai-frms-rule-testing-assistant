import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import Button from '../common/Button'
import '../../styles/pages.css'

const EXPECTED_RESULT_OPTIONS = [
  { value: 'PASS', label: 'Pass (rule behaves as expected)' },
  { value: 'FAIL', label: 'Fail (rule should trigger rejection)' },
]

const EXPECTED_ACTION_OPTIONS = [
  { value: 'ACCEPT',  label: 'Accept' },
  { value: 'MONITOR', label: 'Monitor' },
  { value: 'REJECT',  label: 'Reject' },
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

const STATUS_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const EMPTY = {
  name: '', description: '', scenarioId: '', scenarioName: '', ruleId: '', ruleName: '',
  expectedResult: 'PASS', expectedAction: 'REJECT', status: 'ACTIVE',
  inputCardNumber: '', inputAmount: '', inputMerchantId: '', inputTransactionType: 'PURCHASE',
  inputChannel: 'ONLINE', inputCountry: '',
}

const strVal = (v) => (v == null ? '' : String(v))

const TestCaseForm = ({ initialValues = {}, onSubmit, onCancel, loading = false, submitLabel = 'Save' }) => {
  const init = initialValues
  const inputData = init.inputData ?? {}

  const [form, setForm] = useState({
    ...EMPTY,
    ...init,
    scenarioId: strVal(init.scenarioId),
    ruleId:     strVal(init.ruleId),
    inputCardNumber:      inputData.cardNumber ?? '',
    inputAmount:          inputData.amount != null ? String(inputData.amount) : '',
    inputMerchantId:      inputData.merchantId ?? '',
    inputTransactionType: inputData.transactionType ?? 'PURCHASE',
    inputChannel:         inputData.channel ?? 'ONLINE',
    inputCountry:         inputData.country ?? '',
  })
  const [errors, setErrors] = useState({})

  const set = (e) => {
    const { name, value } = e.target
    setForm((p) => ({ ...p, [name]: value }))
    if (errors[name]) setErrors((p) => ({ ...p, [name]: '' }))
  }

  const validate = () => {
    const e = {}
    if (!form.name.trim())          e.name           = 'Name is required'
    if (!form.expectedResult)       e.expectedResult  = 'Expected result is required'
    if (!form.expectedAction)       e.expectedAction  = 'Expected action is required'
    if (!form.inputCardNumber.trim()) e.inputCardNumber = 'Card number is required'
    if (!form.inputAmount || Number(form.inputAmount) <= 0) e.inputAmount = 'Amount must be > 0'
    if (!form.inputCountry.trim()) e.inputCountry = 'Country is required'
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    onSubmit({
      name: form.name.trim(), description: form.description.trim(),
      scenarioId: form.scenarioId ? Number(form.scenarioId) : null,
      scenarioName: form.scenarioName.trim(),
      ruleId: form.ruleId ? Number(form.ruleId) : null,
      ruleName: form.ruleName.trim(),
      expectedResult: form.expectedResult, expectedAction: form.expectedAction,
      status: form.status,
      inputData: {
        cardNumber: form.inputCardNumber.trim(),
        amount: Number(form.inputAmount),
        merchantId: form.inputMerchantId.trim(),
        transactionType: form.inputTransactionType,
        channel: form.inputChannel,
        country: form.inputCountry.trim().toUpperCase(),
      },
    })
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Basic Information</h3>
        <Input label="Test Case Name" name="name" placeholder="e.g. Credit limit exceeded — should REJECT"
          value={form.name} onChange={set} error={errors.name} required />
        <Input label="Description" name="description" placeholder="What this test case validates"
          value={form.description} onChange={set} />
        <div className="rule-form-grid-3">
          <Input label="Scenario ID" name="scenarioId" placeholder="e.g. 1" value={form.scenarioId} onChange={set} />
          <Input label="Scenario Name" name="scenarioName" placeholder="Scenario name" value={form.scenarioName} onChange={set} />
          <Select label="Status" name="status" options={STATUS_OPTIONS} value={form.status} onChange={set} />
        </div>
        <div className="rule-form-grid-3">
          <Input label="Rule ID" name="ruleId" placeholder="e.g. 1" value={form.ruleId} onChange={set} />
          <Input label="Rule Name" name="ruleName" placeholder="Rule name" value={form.ruleName} onChange={set} />
        </div>
      </div>

      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Transaction Input Data</h3>
        <div className="rule-form-grid-3">
          <Input label="Card Number" name="inputCardNumber" placeholder="16-digit card number"
            value={form.inputCardNumber} onChange={set} error={errors.inputCardNumber} required />
          <Input label="Amount" name="inputAmount" type="number" placeholder="e.g. 5000"
            value={form.inputAmount} onChange={set} error={errors.inputAmount} required />
          <Input label="Merchant ID" name="inputMerchantId" placeholder="e.g. M001"
            value={form.inputMerchantId} onChange={set} />
          <Select label="Transaction Type" name="inputTransactionType" options={TXN_TYPE_OPTIONS}
            value={form.inputTransactionType} onChange={set} />
          <Select label="Channel" name="inputChannel" options={CHANNEL_OPTIONS}
            value={form.inputChannel} onChange={set} />
          <Input label="Country Code" name="inputCountry" placeholder="e.g. US"
            value={form.inputCountry} onChange={set} error={errors.inputCountry} required />
        </div>
      </div>

      <div className="rule-form-section rule-form-section-last">
        <h3 className="rule-form-section-title">Expected Outcome</h3>
        <div className="rule-form-grid-3">
          <Select label="Expected Result" name="expectedResult" options={EXPECTED_RESULT_OPTIONS}
            value={form.expectedResult} onChange={set} error={errors.expectedResult} required />
          <Select label="Expected Action" name="expectedAction" options={EXPECTED_ACTION_OPTIONS}
            value={form.expectedAction} onChange={set} error={errors.expectedAction} required />
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
