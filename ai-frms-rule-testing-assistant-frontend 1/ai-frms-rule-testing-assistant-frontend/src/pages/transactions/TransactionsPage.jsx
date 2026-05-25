import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import Select from '../../components/common/Select'
import Card from '../../components/common/Card'
import ErrorMessage from '../../components/common/ErrorMessage'
import TransactionTable from '../../components/transactions/TransactionTable'
import { useToast } from '../../hooks/useToast'
import transactionService from '../../services/transactionService'
import '../../styles/pages.css'

const STATUS_OPTIONS = [
  { value: 'APPROVED', label: 'Approved' },
  { value: 'DECLINED', label: 'Declined' },
  { value: 'PENDING',  label: 'Pending' },
]

const TYPE_OPTIONS = [
  { value: 'PURCHASE',   label: 'Purchase' },
  { value: 'REFUND',     label: 'Refund' },
  { value: 'WITHDRAWAL', label: 'Withdrawal' },
  { value: 'TRANSFER',   label: 'Transfer' },
]

const TransactionsPage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()

  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [confirmId, setConfirmId] = useState(null)
  const [filters, setFilters] = useState({ search: '', status: '', transactionType: '' })

  const loadTransactions = useCallback(async () => {
    setLoading(true); setError(null)
    try { setTransactions(await transactionService.getAll(filters)) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }, [filters])

  useEffect(() => { loadTransactions() }, [loadTransactions])

  const handleFilterChange = (e) => {
    const { name, value } = e.target
    setFilters((p) => ({ ...p, [name]: value }))
  }

  const handleDelete = async (id) => {
    setDeleting(id)
    try {
      await transactionService.delete(id)
      setTransactions((prev) => prev.filter((t) => t.id !== id))
      addToast('Transaction deleted', 'success')
    } catch (err) { addToast(err.message, 'error') }
    finally { setDeleting(null); setConfirmId(null) }
  }

  return (
    <div>
      <PageHeader
        title="Transactions"
        subtitle={`${transactions.length} transaction${transactions.length !== 1 ? 's' : ''} in the system`}
        actions={
          <Button variant="primary" onClick={() => navigate('/transactions/create')}>
            + Add Transaction
          </Button>
        }
      />

      <div className="rules-filters">
        <div className="rules-search-wrapper">
          <Input name="search" placeholder="Search by ID, merchant or card…" value={filters.search} onChange={handleFilterChange} />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select name="status" placeholder="All Statuses" options={STATUS_OPTIONS} value={filters.status} onChange={handleFilterChange} />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select name="transactionType" placeholder="All Types" options={TYPE_OPTIONS} value={filters.transactionType} onChange={handleFilterChange} />
        </div>
      </div>

      {error ? (
        <ErrorMessage title="Failed to load transactions" message={error} onRetry={loadTransactions} />
      ) : (
        <Card noPadding>
          <TransactionTable
            transactions={transactions}
            loading={loading}
            onView={(id) => navigate(`/transactions/${id}`)}
            onDelete={(id) => setConfirmId(id)}
            confirmId={confirmId}
            deletingId={deleting}
            onConfirmDelete={handleDelete}
            onCancelDelete={() => setConfirmId(null)}
          />
        </Card>
      )}
    </div>
  )
}

export default TransactionsPage
