import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import Button from '../common/Button'
import '../../styles/pages.css'

const TRANSACTION_TYPE_OPTIONS = [
  { value: 'PURCHASE',   label: 'Purchase' },
  { value: 'REFUND',     label: 'Refund' },
  { value: 'WITHDRAWAL', label: 'Withdrawal' },
  { value: 'TRANSFER',   label: 'Transfer' },
]

const TRANSACTION_STATUS_OPTIONS = [
  { value: 'SUCCESS', label: 'Success' },
  { value: 'FAILED',  label: 'Failed' },
  { value: 'PENDING', label: 'Pending' },
]

const CURRENCY_OPTIONS = [
  { value: 'INR', label: 'INR' },
  { value: 'USD', label: 'USD' },
  { value: 'EUR', label: 'EUR' },
  { value: 'GBP', label: 'GBP' },
  { value: 'SGD', label: 'SGD' },
]

// serialNumber and mccCode are required by the backend TransactionDTO.
// Default values are provided so the form is ready for quick test submissions.
const EMPTY = {
  track2Data:        '',
  amount:            '',
  currency:          'INR',
  mid:               '',
  tid:               '',
  mccCode:           '5411',
  serialNumber:      'SN_TXN_001',
  rrn:               '',
  stan:              '',
  transactionType:   'PURCHASE',
  transactionStatus: 'SUCCESS',
  responseCode:      '00',
  responseMessage:   '',
}

const TransactionForm = ({
  initialValues = {},
  onSubmit,
  onCancel,
  loading = false,
  submitLabel = 'Save',
  serverErrors = [],
}) => {
  const [form, setForm] = useState({ ...EMPTY, ...initialValues })
  const [errors, setErrors] = useState({})

  const set = (e) => {
    const { name, value } = e.target
    setForm((p) => ({ ...p, [name]: value }))
    if (errors[name]) setErrors((p) => ({ ...p, [name]: '' }))
  }

  const validate = () => {
    const e = {}
    if (!form.track2Data.trim())  e.track2Data    = 'Card / Track2 data is required'
    if (!form.amount || Number(form.amount) <= 0) e.amount = 'Amount must be greater than 0'
    if (!form.serialNumber.trim()) e.serialNumber = 'Serial number is required'
    if (!form.mccCode.trim())      e.mccCode      = 'MCC code is required'
    if (!form.transactionType)     e.transactionType = 'Transaction type is required'
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    onSubmit({
      amount:            Number(form.amount),
      currency:          form.currency,
      mccCode:           form.mccCode.trim(),
      mid:               form.mid.trim(),
      tid:               form.tid.trim(),
      serialNumber:      form.serialNumber.trim(),
      rrn:               form.rrn.trim(),
      stan:              form.stan.trim(),
      track2Data:        form.track2Data.trim(),
      transactionStatus: form.transactionStatus,
      transactionType:   form.transactionType,
      responseCode:      form.responseCode.trim(),
      responseMessage:   form.responseMessage.trim(),
    })
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {serverErrors.length > 0 && (
        <div style={{
          marginBottom: 16,
          padding: '12px 16px',
          background: '#fee2e2',
          border: '1px solid #fca5a5',
          borderRadius: 8,
        }}>
          <p style={{ fontWeight: 600, color: '#dc2626', marginBottom: 6, marginTop: 0 }}>
            Please fix the following errors:
          </p>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {serverErrors.map((msg, i) => (
              <li key={i} style={{ color: '#dc2626', fontSize: 14 }}>{msg}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Card &amp; Amount</h3>
        <div className="rule-form-grid-3">
          <Input
            label="Card / Track2 Data *"
            name="track2Data"
            placeholder="e.g. 4111111111111111"
            value={form.track2Data}
            onChange={set}
            error={errors.track2Data}
            required
          />
          <Input
            label="Amount *"
            name="amount"
            type="number"
            placeholder="e.g. 150000"
            value={form.amount}
            onChange={set}
            error={errors.amount}
            required
          />
          <Select
            label="Currency"
            name="currency"
            options={CURRENCY_OPTIONS}
            value={form.currency}
            onChange={set}
          />
        </div>
      </div>

      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Terminal &amp; Merchant</h3>
        <div className="rule-form-grid-3">
          <Input
            label="MID (Merchant ID)"
            name="mid"
            placeholder="e.g. MID00001"
            value={form.mid}
            onChange={set}
          />
          <Input
            label="TID (Terminal ID)"
            name="tid"
            placeholder="e.g. TID00001"
            value={form.tid}
            onChange={set}
          />
          <Input
            label="MCC Code *"
            name="mccCode"
            placeholder="e.g. 5411"
            value={form.mccCode}
            onChange={set}
            error={errors.mccCode}
            required
          />
        </div>
        <div className="rule-form-grid-3">
          <Input
            label="Serial Number *"
            name="serialNumber"
            placeholder="e.g. SN_TXN_001"
            value={form.serialNumber}
            onChange={set}
            error={errors.serialNumber}
            required
          />
          <Input
            label="RRN"
            name="rrn"
            placeholder="e.g. RRN123456"
            value={form.rrn}
            onChange={set}
          />
          <Input
            label="STAN"
            name="stan"
            placeholder="e.g. 000001"
            value={form.stan}
            onChange={set}
          />
        </div>
      </div>

      <div className="rule-form-section rule-form-section-last">
        <h3 className="rule-form-section-title">Transaction Details</h3>
        <div className="rule-form-grid-3">
          <Select
            label="Transaction Type *"
            name="transactionType"
            options={TRANSACTION_TYPE_OPTIONS}
            value={form.transactionType}
            onChange={set}
            error={errors.transactionType}
            required
          />
          <Select
            label="Transaction Status"
            name="transactionStatus"
            options={TRANSACTION_STATUS_OPTIONS}
            value={form.transactionStatus}
            onChange={set}
          />
          <Input
            label="Response Code"
            name="responseCode"
            placeholder="e.g. 00"
            value={form.responseCode}
            onChange={set}
          />
        </div>
        <div className="rule-form-grid-3">
          <Input
            label="Response Message"
            name="responseMessage"
            placeholder="e.g. Approved"
            value={form.responseMessage}
            onChange={set}
          />
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

export default TransactionForm
