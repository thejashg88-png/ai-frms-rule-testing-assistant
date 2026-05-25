import React, { useState, useEffect } from 'react'
import Select from '../common/Select'
import Button from '../common/Button'
import AiResponseBox from './AiResponseBox'
import ruleService from '../../services/ruleService'
import aiService from '../../services/aiService'

const AiRuleExplanation = () => {
  const [rules, setRules] = useState([])
  const [selectedRuleId, setSelectedRuleId] = useState('')
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    ruleService.getAll().then(setRules).catch(() => {})
  }, [])

  const options = rules.map((r) => ({ value: String(r.id), label: r.name }))

  const handleExplain = async () => {
    if (!selectedRuleId) return
    setLoading(true); setResponse(null); setError(null)
    try {
      const rule = rules.find((r) => r.id === Number(selectedRuleId))
      const res = await aiService.explainRule(rule)
      setResponse(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 16 }}>
        <div style={{ flex: 1 }}>
          <Select label="Select Rule to Explain" name="rule" placeholder="Choose a rule…"
            options={options} value={selectedRuleId} onChange={(e) => { setSelectedRuleId(e.target.value); setResponse(null) }} />
        </div>
        <div style={{ paddingBottom: 16 }}>
          <Button variant="primary" onClick={handleExplain} loading={loading} disabled={!selectedRuleId}>
            Explain Rule
          </Button>
        </div>
      </div>

      {(loading || response || error) && (
        <div>
          <AiResponseBox loading={loading} error={error}
            content={response ? `${response.explanation}\n\nRisk Level: ${response.riskLevel}\n\nRecommendations:\n${response.recommendations?.map((r, i) => `${i + 1}. ${r}`).join('\n')}` : null} />
        </div>
      )}
    </div>
  )
}

export default AiRuleExplanation
