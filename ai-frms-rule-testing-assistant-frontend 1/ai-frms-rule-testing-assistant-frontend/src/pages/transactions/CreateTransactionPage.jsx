import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import TransactionForm from '../../components/transactions/TransactionForm'
import DummyTransactionGenerator from '../../components/transactions/DummyTransactionGenerator'
import { useToast } from '../../hooks/useToast'
import transactionService from '../../services/transactionService'

const CreateTransactionPage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [prefill, setPrefill] = useState({})
  const [serverErrors, setServerErrors] = useState([])

  const handleSubmit = async (formData) => {
    setLoading(true)
    setServerErrors([])
    try {
      await transactionService.create(formData)
      addToast('Transaction created successfully', 'success')
      navigate('/transactions')
    } catch (err) {
      if (err.validationErrors) {
        setServerErrors(err.validationErrors)
      } else {
        addToast(err.message, 'error')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Add Transaction"
        subtitle="Create a new transaction for rule testing"
        actions={<Button variant="ghost" onClick={() => navigate('/transactions')}>← Back</Button>}
      />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        <DummyTransactionGenerator onGenerated={setPrefill} />
        <Card title="Transaction Details" subtitle="Fill in the transaction data below">
          <TransactionForm
            key={JSON.stringify(prefill)}
            initialValues={prefill}
            onSubmit={handleSubmit}
            onCancel={() => navigate('/transactions')}
            loading={loading}
            submitLabel="Create Transaction"
            serverErrors={serverErrors}
          />
        </Card>
      </div>
    </div>
  )
}

export default CreateTransactionPage
