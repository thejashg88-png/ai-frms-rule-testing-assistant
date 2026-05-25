import React, { useState, useEffect } from 'react'
import Select from '../common/Select'
import Button from '../common/Button'
import AiResponseBox from './AiResponseBox'
import ruleService from '../../services/ruleService'
import aiService from '../../services/aiService'
import { useToast } from '../../hooks/useToast'

const AiGenerateTestCases = () => {
  const { addToast } = useToast()
  const [rules, setRules] = useState([])
  const [selectedRuleId, setSelectedRuleId] = useState('')
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    ruleService.getAll().then(setRules).catch(() => {})
  }, [])

  const options = rules.map((r) => ({ value: String(r.id), label: r.name }))

  const handleGenerate = async () => {
    if (!selectedRuleId) return
    setLoading(true); setResponse(null); setError(null)
    try {
      const rule = rules.find((r) => r.id === Number(selectedRuleId))
      const res = await aiService.generateTestCases(rule.id, rule.name, rule.ruleType)
      setResponse(res)
      addToast(`Generated ${res.generatedTestCases?.length ?? 0} test cases`, 'success')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const formatContent = (res) => {
    if (!res) return null
    const cases = res.generatedTestCases?.map((tc, i) =>
      `Test Case ${i + 1}: ${tc.name}\n  Expected: ${tc.expectedResult} (${tc.expectedAction})\n  ${tc.description}`
    ).join('\n\n')
    return `${res.explanation}\n\n--- Generated Test Cases ---\n\n${cases}`
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 16 }}>
        <div style={{ flex: 1 }}>
          <Select label="Select Rule" name="rule" placeholder="Choose a rule…"
            options={options} value={selectedRuleId} onChange={(e) => { setSelectedRuleId(e.target.value); setResponse(null) }} />
        </div>
        <div style={{ paddingBottom: 16 }}>
          <Button variant="primary" onClick={handleGenerate} loading={loading} disabled={!selectedRuleId}>
            Generate Test Cases
          </Button>
        </div>
      </div>

      <AiResponseBox loading={loading} error={error} content={formatContent(response)} />
    </div>
  )
}

export default AiGenerateTestCases
