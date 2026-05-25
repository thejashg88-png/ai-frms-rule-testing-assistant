import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import Button from '../common/Button'
import '../../styles/pages.css'

const TRANSACTION_TYPE_OPTIONS = [
  { value: 'PURCHASE',    label: 'Purchase' },
  { value: 'REFUND',      label: 'Refund' },
  { value: 'WITHDRAWAL',  label: 'Withdrawal' },
  { value: 'TRANSFER',    label: 'Transfer' },
]

const CHANNEL_OPTIONS = [
  { value: 'ONLINE', label: 'Online' },
  { value: 'POS',    label: 'POS' },
  { value: 'ATM',    label: 'ATM' },
  { value: 'MOBILE', label: 'Mobile' },
]

const CURRENCY_OPTIONS = [
  { value: 'USD', label: 'USD' },
  { value: 'EUR', label: 'EUR' },
  { value: 'GBP', label: 'GBP' },
  { value: 'INR', label: 'INR' },
  { value: 'SGD', label: 'SGD' },
]

const MERCHANT_CATEGORY_OPTIONS = [
  { value: 'E-COMMERCE',  label: 'E-Commerce' },
  { value: 'RETAIL',      label: 'Retail' },
  { value: 'FOOD',        label: 'Food & Dining' },
  { value: 'FUEL',        label: 'Fuel' },
  { value: 'AUTOMOTIVE',  label: 'Automotive' },
  { value: 'TRANSFER',    label: 'Transfer' },
  { value: 'CASH',        label: 'Cash' },
  { value: 'LUXURY',      label: 'Luxury' },
  { value: 'OTHER',       label: 'Other' },
]

const EMPTY = {
  cardNumber: '', amount: '', currency: 'USD',
  merchantId: '', merchantName: '', merchantCategory: '',
  transactionType: 'PURCHASE', channel: 'ONLINE', country: '',
}

const TransactionForm = ({ initialValues = {}, onSubmit, onCancel, loading = false, submitLabel = 'Save' }) => {
  const [form, setForm] = useState({ ...EMPTY, ...initialValues })
  const [errors, setErrors] = useState({})

  const set = (e) => {
    const { name, value } = e.target
    setForm((p) => ({ ...p, [name]: value }))
    if (errors[name]) setErrors((p) => ({ ...p, [name]: '' }))
  }

  const validate = () => {
    const e = {}
    if (!form.cardNumber.trim()) e.cardNumber = 'Card number is required'
    if (!form.amount || Number(form.amount) <= 0) e.amount = 'Amount must be greater than 0'
    if (!form.merchantName.trim()) e.merchantName = 'Merchant name is required'
    if (!form.transactionType) e.transactionType = 'Transaction type is required'
    if (!form.channel) e.channel = 'Channel is required'
    if (!form.country.trim()) e.country = 'Country is required'
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    onSubmit({
      ...form,
      amount: Number(form.amount),
      cardNumber: form.cardNumber.trim(),
      merchantId: form.merchantId.trim() || `M${Date.now()}`,
      merchantName: form.merchantName.trim(),
      country: form.country.trim().toUpperCase(),
    })
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Card &amp; Amount</h3>
        <div className="rule-form-grid-3">
          <Input label="Card Number" name="cardNumber" placeholder="16-digit card number"
            value={form.cardNumber} onChange={set} error={errors.cardNumber} required />
          <Input label="Amount" name="amount" type="number" placeholder="e.g. 1500.00"
            value={form.amount} onChange={set} error={errors.amount} required />
          <Select label="Currency" name="currency" options={CURRENCY_OPTIONS}
            value={form.currency} onChange={set} />
        </div>
      </div>

      <div className="rule-form-section">
        <h3 className="rule-form-section-title">Merchant</h3>
        <div className="rule-form-grid-3">
          <Input label="Merchant ID" name="merchantId" placeholder="e.g. M001"
            value={form.merchantId} onChange={set} />
          <Input label="Merchant Name" name="merchantName" placeholder="e.g. Amazon Inc"
            value={form.merchantName} onChange={set} error={errors.merchantName} required />
          <Select label="Merchant Category" name="merchantCategory" placeholder="Select category"
            options={MERCHANT_CATEGORY_OPTIONS} value={form.merchantCategory} onChange={set} />
        </div>
      </div>

      <div className="rule-form-section rule-form-section-last">
        <h3 className="rule-form-section-title">Transaction Details</h3>
        <div className="rule-form-grid-3">
          <Select label="Transaction Type" name="transactionType" options={TRANSACTION_TYPE_OPTIONS}
            value={form.transactionType} onChange={set} error={errors.transactionType} required />
          <Select label="Channel" name="channel" options={CHANNEL_OPTIONS}
            value={form.channel} onChange={set} error={errors.channel} required />
          <Input label="Country Code" name="country" placeholder="e.g. US"
            value={form.country} onChange={set} error={errors.country} required />
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
