import React, { useState, useEffect } from 'react'
import Select from '../common/Select'
import Button from '../common/Button'
import AiResponseBox from './AiResponseBox'
import ruleService from '../../services/ruleService'
import aiService from '../../services/aiService'
import { useToast } from '../../hooks/useToast'

const normalizeRuleType = (rule) => {
  const name     = (rule?.ruleName || rule?.name || rule?.title || '').toLowerCase()
  const ruleType = (rule?.ruleType || '').toUpperCase()

  if (name.includes('single large'))             return 'SINGLE_LARGE_TX'
  if (name.includes('monthly transaction volume')) return 'MONTHLY_TXN_VOLUME'
  if (name.includes('annual transaction volume'))  return 'ANNUAL_TXN_VOLUME'
  if (name.includes('daily transaction value'))    return 'DAILY_TXN_VALUE'
  if (name.includes('structuring'))               return 'STRUCTURING'
  if (name.includes('unusual'))                   return 'UNUSUAL_AMT'
  if (name.includes('high frequency'))            return 'HIGH_FREQ_TXN'
  if (name.includes('sequential'))               return 'SEQUENTIAL_TXN'
  if (name.includes('round'))                    return 'ROUND_AMT_TXN'
  if (name.includes('velocity'))                 return 'TXN_VELOCITY'
  if (name.includes('abnormal hour'))            return 'ABNORMAL_HOUR'
  if (name.includes('inconsistent mcc'))         return 'INCONSISTENT_MCC'

  if (ruleType === 'AMOUNT')    return 'SINGLE_LARGE_TX'
  if (ruleType === 'FREQUENCY') return 'HIGH_FREQ_TXN'

  return ruleType
}

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
    const rule = rules.find((r) => r.id === Number(selectedRuleId))
    if (!rule?.ruleType) {
      setError('Rule type is required to generate test cases.')
      return
    }
    setLoading(true); setResponse(null); setError(null)
    try {
      const payload = {
        ruleId:              rule.id,
        ruleName:            rule.ruleName     || rule.name  || '',
        ruleType:            normalizeRuleType(rule),
        action:              rule.action       || 'MONITOR',
        txnCount:            rule.txnCount            ?? null,
        txnAmount:           rule.txnAmount           ?? null,
        frequency:           rule.frequency           ?? null,
        maxAmount:           rule.maxAmount           ?? null,
        percentageThreshold: rule.percentageThreshold ?? null,
        description:         rule.ruleDescription || rule.description || '',
      }
      console.log('[AI Generate Test Cases Payload]', payload)
      const res = await aiService.generateTestCases(payload)
      setResponse(res)
      addToast(`Generated ${res.testCases?.length ?? 0} test case${res.testCases?.length === 1 ? '' : 's'}`, 'success')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const formatContent = (res) => {
    if (!res) return null
    const list = res.testCases ?? []
    if (list.length === 0) return 'No test cases were generated.'
    const cases = list.map((tc, i) => {
      const name   = tc.testCaseName || tc.name || `Case ${i + 1}`
      const result = tc.expectedResult || ''
      const desc   = tc.description || ''
      const input  = tc.inputData ? `\n  Input: ${JSON.stringify(tc.inputData, null, 2)}` : ''
      return `Test Case ${i + 1}: ${name}\n  Expected: ${result}\n  ${desc}${input}`
    }).join('\n\n')
    const explanation = res.explanation || ''
    return explanation
      ? `${explanation}\n\n--- Generated Test Cases ---\n\n${cases}`
      : `--- Generated Test Cases ---\n\n${cases}`
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
