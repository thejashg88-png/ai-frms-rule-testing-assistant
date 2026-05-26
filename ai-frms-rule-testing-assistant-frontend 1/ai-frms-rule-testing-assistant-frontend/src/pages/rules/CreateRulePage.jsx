import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle } from 'lucide-react'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import AiRuleGenerator from '../../components/ai/AiRuleGenerator'
import { useToast } from '../../hooks/useToast'
import ruleService from '../../services/ruleService'
import RuleForm from './RuleForm'
import '../../styles/pages.css'

const CreateRulePage = () => {
  const navigate     = useNavigate()
  const { addToast } = useToast()

  const [saving, setSaving]       = useState(false)
  const [formKey, setFormKey]     = useState(0)          // incremented to reset form when AI fills it
  const [aiValues, setAiValues]   = useState(null)       // pre-fill values for RuleForm
  const [aiInsight, setAiInsight] = useState(null)       // explanation + riskNotes

  const normalizeRiskNotes = (rn) => {
    if (Array.isArray(rn)) return rn
    if (typeof rn === 'string' && rn.trim()) return [rn]
    if (rn && typeof rn === 'object') return Object.values(rn).map(String).filter(Boolean)
    return []
  }

  // Called by AiRuleGenerator when a suggestion arrives
  const handleAiGenerate = ({ formValues, explanation, riskNotes }) => {
    setAiValues(formValues)
    setAiInsight({
      explanation: explanation || 'No explanation provided.',
      riskNotes: normalizeRiskNotes(riskNotes),
    })
    setFormKey((k) => k + 1)   // force RuleForm to re-mount with new initialValues
    // scroll form into view
    setTimeout(() => {
      document.getElementById('rule-form-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 100)
  }

  const handleSubmit = async (formData) => {
    setSaving(true)
    try {
      await ruleService.create(formData)
      addToast('Rule created successfully', 'success')
      navigate('/rules')
    } catch (err) {
      addToast(err.message, 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Create Rule"
        subtitle="Define a new FRMS business rule"
        actions={
          <Button variant="ghost" onClick={() => navigate('/rules')}>
            ← Back to Rules
          </Button>
        }
      />

      {/* ── AI Generator ── */}
      <AiRuleGenerator onGenerate={handleAiGenerate} />

      {/* ── AI Insight panel (shown only after a generation) ── */}
      {aiInsight && (
        <div style={{
          background: '#f0fdf4',
          border: '1.5px solid #86efac',
          borderRadius: 12,
          padding: '18px 22px',
          marginBottom: 20,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
            <div style={{
              width: 28, height: 28, borderRadius: 6,
              background: '#16a34a',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0,
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9.663 17h4.673M12 3v1m6.364 1.636-.707.707M21 12h-1M4 12H3m3.343-5.657-.707-.707m2.828 9.9a5 5 0 1 1 7.072 0l-.548.547A3.374 3.374 0 0 0 14 18.469V19a2 2 0 1 1-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
              </svg>
            </div>
            <span style={{ fontWeight: 600, fontSize: 14, color: '#15803d' }}>
              AI Insight — Rule pre-filled below
            </span>
          </div>

          {aiInsight.explanation && (
            <p style={{ margin: '0 0 12px', fontSize: 14, color: '#166534', lineHeight: 1.65 }}>
              {aiInsight.explanation}
            </p>
          )}

          {Array.isArray(aiInsight.riskNotes) && aiInsight.riskNotes.length > 0 && (
            <>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 6,
                marginBottom: 8,
              }}>
                <AlertTriangle size={13} color="#ca8a04" />
                <span style={{
                  fontSize: 11, fontWeight: 700, letterSpacing: '0.06em',
                  color: '#92400e', textTransform: 'uppercase',
                }}>
                  Risk Notes
                </span>
              </div>
              <ul style={{ margin: 0, paddingLeft: 18 }}>
                {aiInsight.riskNotes.map((note, i) => (
                  <li key={i} style={{
                    fontSize: 13, color: '#365314', lineHeight: 1.6,
                    marginBottom: i < aiInsight.riskNotes.length - 1 ? 6 : 0,
                  }}>
                    {note}
                  </li>
                ))}
              </ul>
            </>
          )}

          <div style={{
            marginTop: 14, paddingTop: 12,
            borderTop: '1px solid #bbf7d0',
            fontSize: 12, color: '#166534',
          }}>
            Review the pre-filled form below, make any edits, then click <strong>Create Rule</strong> to save.
          </div>
        </div>
      )}

      {/* ── Rule Form ── */}
      <div id="rule-form-card">
        <Card
          title="Rule Details"
          subtitle={aiValues ? 'AI-generated configuration — review and edit before saving' : 'Fill in the rule configuration below'}
        >
          <RuleForm
            key={formKey}
            initialValues={aiValues ?? {}}
            onSubmit={handleSubmit}
            onCancel={() => navigate('/rules')}
            loading={saving}
            submitLabel="Create Rule"
          />
        </Card>
      </div>
    </div>
  )
}

export default CreateRulePage
