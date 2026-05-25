import React from 'react'
import Table from '../common/Table'
import Button from '../common/Button'
import Badge from '../common/Badge'
import { maskCardNumber } from '../../utils/maskUtils'
import { formatAmount } from '../../utils/amountUtils'

const STATUS_COLORS = {
  APPROVED: { bg: '#dcfce7', color: '#16a34a' },
  DECLINED: { bg: '#fee2e2', color: '#dc2626' },
  PENDING:  { bg: '#fef9c3', color: '#ca8a04' },
}

const TransactionTable = ({ transactions, loading, onView, onDelete, confirmId, deletingId, onConfirmDelete, onCancelDelete }) => {
  const columns = [
    {
      key: 'transactionId',
      label: 'Txn ID',
      width: '140px',
      render: (val) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{val}</span>,
    },
    {
      key: 'cardNumber',
      label: 'Card',
      width: '160px',
      render: (val) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{maskCardNumber(val)}</span>,
    },
    {
      key: 'amount',
      label: 'Amount',
      width: '120px',
      render: (val, row) => <strong>{formatAmount(val, row.currency)}</strong>,
    },
    {
      key: 'merchantName',
      label: 'Merchant',
      render: (val, row) => (
        <div>
          <div style={{ fontWeight: 500 }}>{val}</div>
          <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{row.merchantCategory}</div>
        </div>
      ),
    },
    {
      key: 'transactionType',
      label: 'Type',
      width: '100px',
      render: (val) => <Badge bgColor="#eff6ff" color="#2563eb" size="sm">{val}</Badge>,
    },
    {
      key: 'channel',
      label: 'Channel',
      width: '80px',
      render: (val) => <Badge bgColor="#f8fafc" color="#475569" size="sm">{val}</Badge>,
    },
    {
      key: 'status',
      label: 'Status',
      width: '100px',
      render: (val) => {
        const c = STATUS_COLORS[val] ?? { bg: '#f1f5f9', color: '#475569' }
        return <Badge bgColor={c.bg} color={c.color} size="sm">{val}</Badge>
      },
    },
    {
      key: 'id',
      label: 'Actions',
      width: '150px',
      render: (id) => {
        if (confirmId === id) {
          return (
            <div style={{ display: 'flex', gap: 6 }}>
              <Button variant="danger" size="sm" loading={deletingId === id} onClick={() => onConfirmDelete(id)}>Confirm</Button>
              <Button variant="ghost" size="sm" disabled={deletingId === id} onClick={onCancelDelete}>Cancel</Button>
            </div>
          )
        }
        return (
          <div style={{ display: 'flex', gap: 6 }}>
            <Button variant="outline" size="sm" onClick={() => onView(id)}>View</Button>
            <Button variant="ghost" size="sm" onClick={() => onDelete(id)}>Delete</Button>
          </div>
        )
      },
    },
  ]

  return (
    <Table
      columns={columns}
      data={transactions}
      loading={loading}
      emptyMessage="No transactions found."
    />
  )
}

export default TransactionTable
