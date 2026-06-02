import React, { useState } from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import Button from '../common/Button'
import aiService from '../../services/aiService'
import { useToast } from '../../hooks/useToast'

const CHANNEL_OPTIONS = [{ value: '', label: 'Any' }, { value: 'ONLINE', label: 'Online' }, { value: 'POS', label: 'POS' }, { value: 'ATM', label: 'ATM' }, { value: 'MOBILE', label: 'Mobile' }]
const TYPE_OPTIONS = [{ value: '', label: 'Any' }, { value: 'PURCHASE', label: 'Purchase' }, { value: 'REFUND', label: 'Refund' }, { value: 'WITHDRAWAL', label: 'Withdrawal' }, { value: 'TRANSFER', label: 'Transfer' }]
const COUNTRY_OPTIONS = [{ value: '', label: 'Any' }, { value: 'US', label: 'US' }, { value: 'GB', label: 'UK' }, { value: 'FR', label: 'France' }, { value: 'DE', label: 'Germany' }, { value: 'IN', label: 'India' }, { value: 'SG', label: 'Singapore' }]

const AiTransactionGenerator = ({ onGenerated }) => {
  const { addToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [hints, setHints] = useState({ maxAmount: '', channel: '', transactionType: '', country: '' })
  const [result, setResult] = useState(null)

  const set = (e) => setHints((p) => ({ ...p, [e.target.name]: e.target.value }))

  const generate = async () => {
    setLoading(true); setResult(null)
    try {
      const res = await aiService.generateTransaction({
        maxAmount: hints.maxAmount ? Number(hints.maxAmount) : undefined,
        channel: hints.channel || undefined,
        transactionType: hints.transactionType || undefined,
        country: hints.country || undefined,
      })
      setResult(res)
      onGenerated?.(res)
      addToast('Transaction generated', 'success')
    } catch (err) {
      addToast(err.message, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0 16px' }}>
        <Input label="Max Amount" name="maxAmount" type="number" placeholder="5000" value={hints.maxAmount} onChange={set} />
        <Select label="Channel" name="channel" options={CHANNEL_OPTIONS} value={hints.channel} onChange={set} />
        <Select label="Type" name="transactionType" options={TYPE_OPTIONS} value={hints.transactionType} onChange={set} />
        <Select label="Country" name="country" options={COUNTRY_OPTIONS} value={hints.country} onChange={set} />
      </div>
      <Button variant="primary" onClick={generate} loading={loading}>Generate Transaction</Button>

      {result && (
        <div style={{ marginTop: 16, padding: 16, background: 'var(--color-ai-response-bg)', border: '1px solid var(--color-ai-border)', borderRadius: 8 }}>
          <p style={{ margin: '0 0 8px', fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Generated Transaction</p>
          <pre style={{ margin: 0, fontSize: 12, whiteSpace: 'pre-wrap', color: 'var(--text-primary)' }}>
            {JSON.stringify({ cardNumber: result.cardNumber, amount: result.amount, currency: result.currency, merchant: result.merchantName, type: result.transactionType, channel: result.channel, country: result.country }, null, 2)}
          </pre>
          <p style={{ margin: '8px 0 0', fontSize: 12, color: 'var(--text-secondary)' }}>{result.explanation}</p>
        </div>
      )}
    </div>
  )
}

export default AiTransactionGenerator
