import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import Select from '../../components/common/Select'
import Card from '../../components/common/Card'
import ErrorMessage from '../../components/common/ErrorMessage'
import Pagination from '../../components/common/Pagination'
import TransactionTable from '../../components/transactions/TransactionTable'
import { useToast } from '../../hooks/useToast'
import { useAuth } from '../../hooks/useAuth'
import useClientPagination from '../../hooks/useClientPagination'
import transactionService from '../../services/transactionService'
import '../../styles/pages.css'

// Payment Status — normalized from backend transactionStatus/responseCode.
const STATUS_OPTIONS = [
  { value: 'APPROVED', label: 'Approved' },
  { value: 'DECLINED', label: 'Declined' },
  { value: 'PENDING',  label: 'Pending' },
]

// Risk Status — set by the FRMS rule engine. Separate from payment network status.
const RISK_STATUS_OPTIONS = [
  { value: 'ACCEPT',        label: 'Accept' },
  { value: 'MONITOR',       label: 'Monitor' },
  { value: 'REJECT',        label: 'Reject' },
  { value: 'NOT EVALUATED', label: 'Not Evaluated' },
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
  const { isAdmin } = useAuth()

  // All records fetched from backend (unfiltered)
  const [allTransactions, setAllTransactions] = useState([])
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [confirmId, setConfirmId] = useState(null)

  // Filter state
  const [filters, setFilters] = useState({ search: '', status: '', riskStatus: '', transactionType: '' })

  // Load all records once on mount — no filter params (filtering is client-side)
  const loadTransactions = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const data = await transactionService.getAll()
      setAllTransactions(Array.isArray(data) ? data : [])
    }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { loadTransactions() }, [loadTransactions])

  // ── Client-side filtering ──────────────────────────────────────────────────
  const filteredTransactions = allTransactions.filter((t) => {
    if (filters.status          && t.status          !== filters.status)          return false
    if (filters.riskStatus      && t.riskStatus      !== filters.riskStatus)      return false
    if (filters.transactionType && t.transactionType !== filters.transactionType) return false
    if (filters.search) {
      const q = filters.search.toLowerCase()
      if (
        !(t.transactionId || '').toLowerCase().includes(q) &&
        !(t.merchantName  || '').toLowerCase().includes(q) &&
        !(t.cardNumber    || '').slice(-4).includes(q)
      ) return false
    }
    return true
  })

  console.log('[Transactions] total', allTransactions.length, 'filtered', filteredTransactions.length)

  // ── Pagination ─────────────────────────────────────────────────────────────
  const {
    currentPage, pageSize, totalItems,
    paginatedItems: paginatedTransactions,
    setCurrentPage, setPageSize,
  } = useClientPagination(filteredTransactions)

  // Reset to page 1 whenever a filter changes
  useEffect(() => {
    setCurrentPage(1)
  }, [filters.search, filters.status, filters.riskStatus, filters.transactionType]) // eslint-disable-line react-hooks/exhaustive-deps

  // ── Handlers ───────────────────────────────────────────────────────────────
  const handleFilterChange = (e) => {
    const { name, value } = e.target
    setFilters((p) => ({ ...p, [name]: value }))
  }

  const handleDelete = async (id) => {
    setDeleting(id)
    try {
      await transactionService.delete(id)
      setAllTransactions((prev) => prev.filter((t) => t.id !== id))
      addToast('Transaction deleted', 'success')
    } catch (err) { addToast(err.message, 'error') }
    finally { setDeleting(null); setConfirmId(null) }
  }

  // ── Subtitle text ──────────────────────────────────────────────────────────
  const hasFilter    = filters.search || filters.status || filters.riskStatus || filters.transactionType
  const subtitleText = hasFilter
    ? `${filteredTransactions.length} of ${allTransactions.length} transaction${allTransactions.length !== 1 ? 's' : ''} match`
    : `${allTransactions.length} transaction${allTransactions.length !== 1 ? 's' : ''} in the system`

  return (
    <div>
      <PageHeader
        title="Transactions"
        subtitle={subtitleText}
        actions={isAdmin && (
          <Button variant="primary" onClick={() => navigate('/transactions/create')}>
            + Add Transaction
          </Button>
        )}
      />

      {/* ── Filters ── */}
      <div className="rules-filters">
        <div className="rules-search-wrapper">
          <Input name="search" placeholder="Search by ID, merchant or card…" value={filters.search} onChange={handleFilterChange} />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select name="status" placeholder="All Payment Statuses" options={STATUS_OPTIONS} value={filters.status} onChange={handleFilterChange} />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select name="riskStatus" placeholder="All Risk Statuses" options={RISK_STATUS_OPTIONS} value={filters.riskStatus} onChange={handleFilterChange} />
        </div>
        <div className="rules-filter-select-wrapper">
          <Select name="transactionType" placeholder="All Types" options={TYPE_OPTIONS} value={filters.transactionType} onChange={handleFilterChange} />
        </div>
      </div>

      {error ? (
        <ErrorMessage title="Failed to load transactions" message={error} onRetry={loadTransactions} />
      ) : (
        <>
          <Card noPadding>
            <TransactionTable
              transactions={paginatedTransactions}
              loading={loading}
              onView={(id) => navigate(`/transactions/${id}`)}
              onDelete={(id) => setConfirmId(id)}
              confirmId={confirmId}
              deletingId={deleting}
              onConfirmDelete={handleDelete}
              onCancelDelete={() => setConfirmId(null)}
            />
          </Card>

          {!loading && (
            <Pagination
              currentPage={currentPage}
              pageSize={pageSize}
              totalItems={totalItems}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          )}
        </>
      )}
    </div>
  )
}

export default TransactionsPage
