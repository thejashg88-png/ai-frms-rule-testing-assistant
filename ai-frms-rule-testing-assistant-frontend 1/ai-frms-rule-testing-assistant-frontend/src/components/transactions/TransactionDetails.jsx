import React from 'react'
import Badge from '../common/Badge'
import { maskCardNumber } from '../../utils/maskUtils'
import { formatAmount } from '../../utils/amountUtils'

const STATUS_COLORS = {
  APPROVED: { bg: '#dcfce7', color: '#16a34a' },
  DECLINED: { bg: '#fee2e2', color: '#dc2626' },
  PENDING:  { bg: '#fef9c3', color: '#ca8a04' },
}

const Row = ({ label, value }) => (
  <div style={{ display: 'flex', gap: 16, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
    <span style={{ width: 180, flexShrink: 0, fontSize: 13, color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
    <span style={{ fontSize: 14, color: 'var(--text-primary)' }}>{value ?? '—'}</span>
  </div>
)

const TransactionDetails = ({ transaction }) => {
  if (!transaction) return null
  const sc = STATUS_COLORS[transaction.status] ?? { bg: '#f1f5f9', color: '#475569' }

  return (
    <div>
      <Row label="Transaction ID" value={<code style={{ fontSize: 13 }}>{transaction.transactionId}</code>} />
      <Row label="Card Number" value={<code>{maskCardNumber(transaction.cardNumber)}</code>} />
      <Row label="Amount" value={<strong>{formatAmount(transaction.amount, transaction.currency)}</strong>} />
      <Row label="Merchant" value={`${transaction.merchantName} (${transaction.merchantId})`} />
      <Row label="Merchant Category" value={transaction.merchantCategory} />
      <Row label="Transaction Type" value={transaction.transactionType} />
      <Row label="Channel" value={transaction.channel} />
      <Row label="Country" value={transaction.country} />
      <Row label="Status" value={
        <Badge bgColor={sc.bg} color={sc.color} size="sm">{transaction.status}</Badge>
      } />
      <Row label="Created" value={transaction.createdAt ? new Date(transaction.createdAt).toLocaleString() : null} />
    </div>
  )
}

export default TransactionDetails
