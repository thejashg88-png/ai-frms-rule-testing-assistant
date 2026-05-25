import React from 'react'
import { maskCardNumber } from '../../utils/maskUtils'
import { formatAmount } from '../../utils/amountUtils'

const Field = ({ label, value }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '7px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
    <span style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
    <span style={{ color: 'var(--text-primary)', fontFamily: label === 'Card' ? 'monospace' : undefined }}>{value ?? '—'}</span>
  </div>
)

const TestInputDataBox = ({ inputData = {} }) => {
  return (
    <div style={{ background: 'var(--bg-secondary)', borderRadius: 8, padding: '16px', border: '1px solid var(--border)' }}>
      <p style={{ margin: '0 0 12px', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
        Test Input Data
      </p>
      <Field label="Card" value={maskCardNumber(inputData.cardNumber)} />
      <Field label="Amount" value={formatAmount(inputData.amount, inputData.currency)} />
      <Field label="Merchant" value={inputData.merchantId} />
      <Field label="Txn Type" value={inputData.transactionType} />
      <Field label="Channel" value={inputData.channel} />
      <Field label="Country" value={inputData.country} />
    </div>
  )
}

export default TestInputDataBox
