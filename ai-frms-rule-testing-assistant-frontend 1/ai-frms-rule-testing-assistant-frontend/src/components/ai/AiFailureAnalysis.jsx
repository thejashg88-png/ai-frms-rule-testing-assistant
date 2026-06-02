import React, { useState, useEffect } from 'react'
import Select from '../common/Select'
import Button from '../common/Button'
import AiResponseBox from './AiResponseBox'
import executionService from '../../services/executionService'
import aiService from '../../services/aiService'

const cleanListItemText = (value) => {
  if (!value) return ''
  return String(value)
    .replace(/^\s*\d+[.)]\s*/, '')
    .replace(/^\s*[-•]\s*/, '')
    .trim()
}

const cleanArray = (items) =>
  (Array.isArray(items) ? items : [])
    .map(cleanListItemText)
    .filter(Boolean)

const AiFailureAnalysis = () => {
  const [executions, setExecutions] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    executionService.getAll()
      .then((all) => setExecutions(all.filter((e) => e.normalizedStatus === 'FAILED')))
      .catch(() => {})
  }, [])

  const options = executions.map((e) => ({ value: String(e.id), label: `#${e.id} — ${e.entityName}` }))

  const handleAnalyze = async () => {
    if (!selectedId) return
    setLoading(true)
    setResponse(null)
    setError(null)

    try {
      const selectedExecution = executions.find((e) => e.id === Number(selectedId))
      const firstResult = selectedExecution?.results?.[0] || {}
      const comparison  = firstResult?.comparisonResult   || {}

      console.log('[AI Failure Analysis Selected Execution]', selectedExecution)
      console.log('[AI Failure Analysis First Result]', firstResult)
      console.log('[AI Failure Analysis Comparison]', comparison)

      const ruleType =
        firstResult.ruleType       ||
        comparison.ruleType        ||
        comparison.actualRuleType  ||
        selectedExecution.ruleType ||
        selectedExecution.actualRuleType ||
        ''

      if (!ruleType) {
        setError('Rule type is missing for this execution. Please re-run the test case or check execution response.')
        return
      }

      const actualAction =
        firstResult.actualAction        ||
        comparison.actualAction         ||
        selectedExecution.actualAction  ||
        selectedExecution.result        ||
        selectedExecution.executionStatus ||
        ''

      const executionLogs =
        firstResult.failureReason       ||
        comparison.failureReason        ||
        firstResult.message             ||
        comparison.message              ||
        selectedExecution.failureReason ||
        selectedExecution.message       ||
        'Execution failed or actual result did not match expected result'

      const expectedResult = {
        expectedOutcome:
          comparison.expectedOutcome          ||
          firstResult.expectedOutcome         ||
          selectedExecution.expectedOutcome   ||
          'FAIL',
        expectedAction:
          comparison.expectedAction           ||
          firstResult.expectedAction          ||
          selectedExecution.expectedAction    ||
          '',
        expectedRiskLevel:
          comparison.expectedRiskLevel        ||
          firstResult.expectedRiskLevel       ||
          selectedExecution.expectedRiskLevel ||
          'HIGH',
        expectedEvaluationStatus:
          comparison.expectedEvaluationStatus ||
          firstResult.expectedEvaluationStatus ||
          null,
        expectedRuleType:
          comparison.expectedRuleType         ||
          firstResult.expectedRuleType        ||
          ruleType,
        expectedAlertCodes:
          comparison.expectedAlertCodes       ||
          firstResult.expectedAlertCodes      ||
          null,
        expectedRiskScore:
          comparison.expectedRiskScore        ||
          firstResult.expectedRiskScore       ||
          null,
        remarks: 'Generated from failed execution for AI failure analysis',
      }

      console.log('[AI Failure Analysis Expected Result Object]', expectedResult)

      if (!expectedResult.expectedAction) {
        setError('Expected action is missing for this execution. Please re-run the test case or check execution response.')
        return
      }

      const payload = {
        executionId:  selectedExecution.executionId || selectedExecution.id,
        testCaseName:
          firstResult.testCaseName        ||
          selectedExecution.testCaseName  ||
          selectedExecution.entityName    ||
          selectedExecution.name          ||
          '',
        ruleType,
        expectedResult,
        actualResult:   actualAction,
        inputData:      selectedExecution.inputData || firstResult.inputData || {},
        executionLogs,
      }

      console.log('[AI Failure Analysis Payload]', payload)

      const res = await aiService.analyzeFailure(payload)
      setResponse(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const formatContent = (res) => {
    if (!res) return null
    const lines = []

    lines.push(`Summary: ${res.summary || 'No failure summary available.'}`)
    lines.push(`\nRoot Cause: ${res.rootCause || 'No root cause identified.'}`)

    const suggestions = cleanArray(res.suggestions)
    const suggestionList = suggestions.length
      ? suggestions.map((s, i) => `  ${i + 1}. ${s}`).join('\n')
      : '  No suggestions available.'
    lines.push(`\nSuggestions:\n${suggestionList}`)

    const possibleReasons = cleanArray(res.possibleReasons)
    if (possibleReasons.length) {
      lines.push(`\nPossible Reasons:\n${possibleReasons.map((r, i) => `  ${i + 1}. ${r}`).join('\n')}`)
    }

    const debuggingSteps = cleanArray(res.debuggingSteps)
    if (debuggingSteps.length) {
      lines.push(`\nDebugging Steps:\n${debuggingSteps.map((s, i) => `  ${i + 1}. ${s}`).join('\n')}`)
    }

    lines.push(`\nRecommended Fix: ${res.recommendedFix || 'No recommended fix available.'}`)
    lines.push(`\nRisk Impact: ${res.riskImpact || 'No risk impact available.'}`)
    lines.push(`\nConfidence: ${res.confidence ?? 0}%`)

    return lines.join('\n')
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 16 }}>
        <div style={{ flex: 1 }}>
          <Select
            label="Select Failed Execution"
            name="execution"
            placeholder="Choose a failed execution…"
            options={options}
            value={selectedId}
            onChange={(e) => { setSelectedId(e.target.value); setResponse(null); setError(null) }}
          />
        </div>
        <div style={{ paddingBottom: 16 }}>
          <Button variant="primary" onClick={handleAnalyze} loading={loading} disabled={!selectedId}>
            Analyze Failure
          </Button>
        </div>
      </div>

      <AiResponseBox loading={loading} error={error} content={formatContent(response)} />
    </div>
  )
}

export default AiFailureAnalysis
