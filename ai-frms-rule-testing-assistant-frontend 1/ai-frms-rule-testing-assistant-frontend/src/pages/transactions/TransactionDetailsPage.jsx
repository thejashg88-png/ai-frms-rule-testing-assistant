import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import TransactionDetails from '../../components/transactions/TransactionDetails'
import transactionService from '../../services/transactionService'

const TransactionDetailsPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [transaction, setTransaction] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setLoading(true); setError(null)
    transactionService.getById(id)
      .then(setTransaction)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <Loader message="Loading transaction…" />
  if (error) return <ErrorMessage title="Failed to load transaction" message={error} onRetry={() => window.location.reload()} />

  return (
    <div>
      <PageHeader
        title="Transaction Details"
        subtitle={transaction?.transactionId}
        actions={<Button variant="ghost" onClick={() => navigate('/transactions')}>← Back</Button>}
      />
      <Card title="Transaction Information">
        <TransactionDetails transaction={transaction} />
      </Card>
    </div>
  )
}

export default TransactionDetailsPage
