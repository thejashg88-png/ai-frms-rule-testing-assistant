import React, { useState } from 'react'
import { Sparkles } from 'lucide-react'
import TextArea from '../common/TextArea'
import Button from '../common/Button'
import aiService from '../../services/aiService'

const normalizeRiskNotes = (rn) => {
  if (Array.isArray(rn)) return rn
  if (typeof rn === 'string' && rn.trim()) return [rn]
  if (rn && typeof rn === 'object') return Object.values(rn).map(String).filter(Boolean)
  return []
}

const mapToFormValues = (data) => ({
  name:                data.ruleName              ?? '',
  description:         data.ruleDescription       ?? '',
  ruleType:            data.ruleType              ?? '',
  action:              data.action                ?? '',
  status:              data.status                ?? 'ACTIVE',
  txnCount:            data.txnCount       != null ? String(data.txnCount)                     : '',
  txnAmount:           data.txnAmount      != null ? String(Number(data.txnAmount))             : '',
  frequency:           data.frequency      != null ? String(data.frequency)                     : '',
  maxAmount:           data.maxAmount      != null ? String(data.maxAmount)                     : '',
  percentageThreshold: data.percentageThreshold != null ? String(data.percentageThreshold)      : '',
})

const AiRuleGenerator = ({ onGenerate }) => {
  const [requirement, setRequirement] = useState('')
  const [loading, setLoading]         = useState(false)
  const [error, setError]             = useState(null)

  const handleGenerate = async () => {
    const trimmed = requirement.trim()
    if (!trimmed) return
    setLoading(true)
    setError(null)
    try {
      const suggestion = await aiService.generateRule(trimmed)
      onGenerate({
        formValues:  mapToFormValues(suggestion),
        explanation: suggestion.explanation ?? null,
        riskNotes:   normalizeRiskNotes(suggestion.riskNotes),
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) handleGenerate()
  }

  return (
    <div style={{
      background: 'linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%)',
      border: '1.5px solid #bfdbfe',
      borderRadius: 12,
      padding: '20px 24px',
      marginBottom: 20,
    }}>
      {/* ── Header ── */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
        <div style={{
          width: 36, height: 36, borderRadius: 8,
          background: 'var(--primary)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0,
        }}>
          <Sparkles size={18} color="white" />
        </div>
        <div>
          <div style={{ fontWeight: 600, fontSize: 15, color: 'var(--text-primary)', lineHeight: 1.3 }}>
            AI Rule Generator
          </div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 1 }}>
            Describe your requirement in plain English — AI will suggest the rule configuration
          </div>
        </div>
        <div style={{
          marginLeft: 'auto', fontSize: 11, fontWeight: 600, letterSpacing: '0.05em',
          color: '#2563eb', background: '#dbeafe', borderRadius: 6, padding: '3px 8px',
        }}>
          OPTIONAL
        </div>
      </div>

      {/* ── Textarea ── */}
      <TextArea
        label="Rule requirement"
        placeholder={'e.g. "Monitor accounts with more than 3 transactions below ₹50,000 within 24 hours"\n\nTip: Ctrl+Enter to generate.'}
        rows={3}
        value={requirement}
        onChange={(e) => { setRequirement(e.target.value); if (error) setError(null) }}
        onKeyDown={handleKeyDown}
        disabled={loading}
      />

      {/* ── Actions ── */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 4 }}>
        <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
          {loading ? 'AI is generating the rule configuration…' : 'The form below will be auto-filled. You can edit before saving.'}
        </span>
        <Button
          variant="primary"
          onClick={handleGenerate}
          loading={loading}
          disabled={!requirement.trim() || loading}
        >
          Generate Rule with AI
        </Button>
      </div>

      {/* ── Error ── */}
      {error && (
        <div style={{
          marginTop: 14, padding: '10px 14px',
          background: '#fee2e2', border: '1px solid #fca5a5',
          borderRadius: 8, color: '#dc2626', fontSize: 13, lineHeight: 1.5,
        }}>
          <strong>Generation failed:</strong> {error}
        </div>
      )}
    </div>
  )
}

export default AiRuleGenerator
