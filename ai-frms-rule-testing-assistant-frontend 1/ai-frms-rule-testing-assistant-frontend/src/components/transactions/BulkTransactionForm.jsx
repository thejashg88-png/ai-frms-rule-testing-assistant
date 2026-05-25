import React, { useState } from 'react'
import TextArea from '../common/TextArea'
import Button from '../common/Button'

const SAMPLE = JSON.stringify([
  {
    cardNumber: '4532015112830366', amount: 1500, currency: 'USD',
    merchantId: 'M001', merchantName: 'Amazon Inc', merchantCategory: 'E-COMMERCE',
    transactionType: 'PURCHASE', channel: 'ONLINE', country: 'US',
  },
], null, 2)

const BulkTransactionForm = ({ onSubmit, loading = false }) => {
  const [json, setJson] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    try {
      const parsed = JSON.parse(json)
      if (!Array.isArray(parsed)) { setError('Input must be a JSON array'); return }
      setError('')
      onSubmit(parsed)
    } catch {
      setError('Invalid JSON format')
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <TextArea
        label="Transactions JSON Array"
        placeholder={SAMPLE}
        value={json}
        onChange={(e) => { setJson(e.target.value); setError('') }}
        rows={12}
        error={error}
      />
      <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
        <Button type="submit" variant="primary" loading={loading}>
          Import Transactions
        </Button>
        <Button type="button" variant="ghost" onClick={() => setJson(SAMPLE)}>
          Load Sample
        </Button>
      </div>
    </form>
  )
}

export default BulkTransactionForm
