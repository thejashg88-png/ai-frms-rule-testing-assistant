import React from 'react'
import Badge from '../common/Badge'
import { maskCardNumber } from '../../utils/maskUtils'
import { formatAmount } from '../../utils/amountUtils'

const PAYMENT_STATUS_COLORS = {
  APPROVED: { bg: '#dcfce7', color: '#16a34a' },
  DECLINED: { bg: '#fee2e2', color: '#dc2626' },
  PENDING:  { bg: '#fef9c3', color: '#ca8a04' },
}

const RISK_STATUS_COLORS = {
  ACCEPT:          { bg: '#dcfce7', color: '#15803d' },
  MONITOR:         { bg: '#fff7ed', color: '#c2410c' },
  REJECT:          { bg: '#fee2e2', color: '#dc2626' },
  'NOT EVALUATED': { bg: '#f1f5f9', color: '#64748b' },
}

const Row = ({ label, value }) => (
  <div style={{ display: 'flex', gap: 16, padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
    <span style={{ width: 200, flexShrink: 0, fontSize: 13, color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
    <span style={{ fontSize: 14, color: 'var(--text-primary)' }}>{value ?? '—'}</span>
  </div>
)

const SectionHeader = ({ title }) => (
  <div style={{ margin: '20px 0 4px', fontSize: 11, fontWeight: 700, letterSpacing: '0.07em', textTransform: 'uppercase', color: 'var(--text-secondary)' }}>
    {title}
  </div>
)

const TransactionDetails = ({ transaction }) => {
  if (!transaction) return (
    <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: 14 }}>Transaction details not available.</p>
  )

  const sc  = PAYMENT_STATUS_COLORS[transaction.status]     ?? { bg: '#f1f5f9', color: '#475569' }
  const rsc = RISK_STATUS_COLORS[transaction.riskStatus]    ?? RISK_STATUS_COLORS['NOT EVALUATED']
  const riskStatus = transaction.riskStatus ?? 'NOT EVALUATED'

  return (
    <div>
      <SectionHeader title="Payment Information" />
      <Row label="Transaction ID"   value={<code style={{ fontSize: 13 }}>{transaction.transactionId}</code>} />
      <Row label="Card Number"      value={<code>{maskCardNumber(transaction.cardNumber)}</code>} />
      <Row label="Amount"           value={<strong>{formatAmount(transaction.amount, transaction.currency)}</strong>} />
      <Row label="Merchant"         value={transaction.merchantName ? `${transaction.merchantName}${transaction.merchantId ? ` (${transaction.merchantId})` : ''}` : null} />
      <Row label="Merchant Category" value={transaction.merchantCategory} />
      <Row label="Transaction Type" value={transaction.transactionType} />
      <Row label="Channel"          value={transaction.channel} />
      <Row label="Country"          value={transaction.country} />
      <Row label="Created"          value={transaction.createdAt ? new Date(transaction.createdAt).toLocaleString() : null} />

      <SectionHeader title="Payment Status" />
      <Row label="Payment Status" value={
        <Badge bgColor={sc.bg} color={sc.color} size="sm">{transaction.status ?? '—'}</Badge>
      } />
      {transaction.responseCode && (
        <Row label="Response Code"    value={<code style={{ fontSize: 13 }}>{transaction.responseCode}</code>} />
      )}
      {transaction.responseMessage && (
        <Row label="Response Message" value={transaction.responseMessage} />
      )}

      <SectionHeader title="FRMS Risk Evaluation" />
      <Row label="Risk Status" value={
        <Badge bgColor={rsc.bg} color={rsc.color} size="sm">{riskStatus}</Badge>
      } />
      {transaction.triggeredRuleName && (
        <Row label="Triggered Rule"      value={transaction.triggeredRuleName} />
      )}
      {transaction.triggeredRuleType && (
        <Row label="Triggered Rule Type" value={transaction.triggeredRuleType} />
      )}
      {transaction.triggeredAction && (
        <Row label="Triggered Action"    value={transaction.triggeredAction} />
      )}
      {transaction.riskReason && (
        <Row label="Risk Reason"         value={transaction.riskReason} />
      )}
      {!transaction.triggeredRuleName && !transaction.riskReason && (
        <Row label="Notes" value="No rule triggered for this transaction." />
      )}
    </div>
  )
}

export default TransactionDetails
