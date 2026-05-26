import React, { useState, useEffect } from 'react'
import Select from '../common/Select'
import Button from '../common/Button'
import AiResponseBox from './AiResponseBox'
import ruleService from '../../services/ruleService'
import aiService from '../../services/aiService'

const RISK_COLORS = {
  HIGH:   { bg: '#fef2f2', color: '#b91c1c', border: '#fca5a5' },
  MEDIUM: { bg: '#fffbeb', color: '#b45309', border: '#fcd34d' },
  LOW:    { bg: '#f0fdf4', color: '#15803d', border: '#86efac' },
}

const Section = ({ label, children }) => (
  <div style={{ marginBottom: 18 }}>
    <div style={{
      fontSize: 11, fontWeight: 700, letterSpacing: '0.07em',
      color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: 6,
    }}>
      {label}
    </div>
    {children}
  </div>
)

const TextBlock = ({ text }) => (
  <p style={{ margin: 0, fontSize: 14, color: 'var(--text-primary)', lineHeight: 1.75 }}>
    {text}
  </p>
)

const BulletList = ({ items, emptyText }) => (
  Array.isArray(items) && items.length > 0 ? (
    <ul style={{ margin: 0, paddingLeft: 20 }}>
      {items.map((item, i) => (
        <li key={i} style={{
          fontSize: 14, color: 'var(--text-primary)', lineHeight: 1.75,
          marginBottom: i < items.length - 1 ? 4 : 0,
        }}>
          {item}
        </li>
      ))}
    </ul>
  ) : (
    <p style={{ margin: 0, fontSize: 14, color: 'var(--text-secondary)' }}>{emptyText}</p>
  )
)

const AiRuleExplanation = () => {
  const [rules, setRules]               = useState([])
  const [selectedRuleId, setSelectedRuleId] = useState('')
  const [loading, setLoading]           = useState(false)
  const [response, setResponse]         = useState(null)
  const [error, setError]               = useState(null)

  useEffect(() => {
    ruleService.getAll().then(setRules).catch(() => {})
  }, [])

  const options = rules.map((r) => ({ value: String(r.id), label: r.name }))

  const handleExplain = async () => {
    if (!selectedRuleId) return
    setLoading(true); setResponse(null); setError(null)
    try {
      const rule = rules.find((r) => r.id === Number(selectedRuleId))
      const res  = await aiService.explainRule(rule)
      console.log('[AI component props]', res)
      setResponse(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const riskLevel   = response?.riskLevel || 'MEDIUM'
  const riskColors  = RISK_COLORS[riskLevel] ?? RISK_COLORS.MEDIUM

  return (
    <div>
      {/* ── Rule selector ── */}
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 20 }}>
        <div style={{ flex: 1 }}>
          <Select
            label="Select Rule to Explain"
            name="rule"
            placeholder="Choose a rule…"
            options={options}
            value={selectedRuleId}
            onChange={(e) => { setSelectedRuleId(e.target.value); setResponse(null) }}
          />
        </div>
        <div style={{ paddingBottom: 16 }}>
          <Button variant="primary" onClick={handleExplain} loading={loading} disabled={!selectedRuleId}>
            Explain Rule
          </Button>
        </div>
      </div>

      {/* ── Loading / Error ── */}
      {(loading || error) && (
        <AiResponseBox loading={loading} error={error} content={null} />
      )}

      {/* ── Structured AI response — all 8 sections always rendered ── */}
      {!loading && !error && response && (
        <div style={{
          background: '#f8fafc',
          border: '1px solid var(--border)',
          borderRadius: 10,
          padding: '20px 24px',
        }}>

          {/* Header + Risk Level badge */}
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            marginBottom: 20, paddingBottom: 16,
            borderBottom: '1px solid var(--border)',
          }}>
            <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>
              AI Rule Explanation
            </span>
            <span style={{
              fontSize: 12, fontWeight: 700, letterSpacing: '0.06em',
              padding: '4px 12px', borderRadius: 6,
              background: riskColors.bg,
              color: riskColors.color,
              border: `1px solid ${riskColors.border}`,
            }}>
              {riskLevel} RISK
            </span>
          </div>

          {/* 1. Explanation */}
          <Section label="Explanation">
            <TextBlock text={response.explanation} />
          </Section>

          {/* 2. Summary */}
          <Section label="Summary">
            <TextBlock text={response.summary} />
          </Section>

          {/* 3. Business Meaning */}
          <Section label="Business Meaning">
            <TextBlock text={response.businessMeaning} />
          </Section>

          {/* 4. Technical Meaning */}
          <Section label="Technical Meaning">
            <TextBlock text={response.technicalMeaning} />
          </Section>

          {/* 5. Example Scenario */}
          <Section label="Example Scenario">
            <TextBlock text={response.exampleScenario} />
          </Section>

          {/* 6. Recommendations */}
          <Section label="Recommendations">
            <BulletList items={response.recommendations} emptyText="No recommendations available." />
          </Section>

          {/* 7. Risk Notes */}
          <Section label="Risk Notes">
            <BulletList items={response.riskNotes} emptyText="No risk notes available." />
          </Section>

        </div>
      )}
    </div>
  )
}

export default AiRuleExplanation
