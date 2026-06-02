import React from 'react'
import { maskCardNumber } from '../../utils/maskUtils'
import { formatAmount } from '../../utils/amountUtils'

const Field = ({ label, value }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '7px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
    <span style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
    <span style={{ color: 'var(--text-primary)', fontFamily: label === 'Card' ? 'monospace' : undefined }}>{value ?? '—'}</span>
  </div>
)

const TestInputDataBox = ({ inputData }) => {
  const data = inputData ?? {}
  const currency = typeof data.currency === 'string' && data.currency.trim() ? data.currency : 'INR'
  const amount = data.amount ?? null

  console.log('[TestInputDataBox] inputData', data)

  return (
    <div style={{ background: 'var(--bg-secondary)', borderRadius: 8, padding: '16px', border: '1px solid var(--border)' }}>
      <p style={{ margin: '0 0 12px', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
        Test Input Data
      </p>
      <Field label="Card" value={maskCardNumber(data.cardNumber)} />
      <Field label="Amount" value={formatAmount(amount, currency)} />
      <Field label="Merchant" value={data.merchantId} />
      <Field label="Txn Type" value={data.transactionType} />
      <Field label="Channel" value={data.channel} />
      <Field label="Country" value={data.country} />
    </div>
  )
}

export default TestInputDataBox
