import React, { useState, useEffect } from 'react'
import Select from '../common/Select'
import Button from '../common/Button'
import testCaseService from '../../services/testCaseService'
import executionService from '../../services/executionService'
import { useToast } from '../../hooks/useToast'
import PassFailBadge from './PassFailBadge'
import FailureReasonBox from './FailureReasonBox'
import RuleExplanationPanel, { getRuleExplanation } from '../common/RuleExplanationPanel'
import ExecutionTracePanel, { getExecutionTrace } from '../common/ExecutionTracePanel'
import transactionService from '../../services/transactionService'
import { requiresHistory } from '../../data/ruleTypes'

const ExecuteTestCasePanel = ({ onExecuted }) => {
  const { addToast } = useToast()
  const [testCases, setTestCases] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [running, setRunning] = useState(false)
  const [result, setResult] = useState(null)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyResult, setHistoryResult] = useState(null)
  const [historyError, setHistoryError] = useState(null)
  const selectedTc = testCases.find((t) => t.id === Number(selectedId)) ?? null
  const ruleType = selectedTc?.expectedResult?.expectedRuleType || selectedTc?.ruleType || selectedTc?.rule?.ruleType || null

  const handleGenerateHistory = async () => {
    if (!selectedTc) return
    const testCaseId = selectedTc.testCaseId || selectedTc.id || Number(selectedId)
    setHistoryLoading(true)
    setHistoryResult(null)
    setHistoryError(null)
    try {
      const res = await transactionService.generateRequiredHistory(testCaseId)
      setHistoryResult(res)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data?.error || err?.message || 'Failed to generate history'
      if (typeof msg === 'string' && msg.toLowerCase().includes('already exists')) {
        setHistoryResult({ message: msg, generatedCount: 0, alreadyExists: true })
      } else {
        setHistoryError(msg)
      }
    } finally {
      setHistoryLoading(false)
    }
  }

  useEffect(() => {
    testCaseService.getAll().then(setTestCases).catch(() => {})
  }, [])

  const options = testCases.map((t) => ({ value: String(t.id), label: t.name }))

  const handleRun = async () => {
    if (!selectedId) return
    setRunning(true); setResult(null)
    try {
      const tc = testCases.find((t) => t.id === Number(selectedId))
      const execution = await executionService.runTestCase(selectedId, tc)
      setResult(execution)
      addToast(`Test case ${execution.status}`, execution.status === 'PASSED' ? 'success' : 'error')
      onExecuted?.(execution)
    } catch (err) { addToast(err.message, 'error') }
    finally { setRunning(false) }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 16 }}>
        <div style={{ flex: 1 }}>
          <Select label="Select Test Case" name="testCase" placeholder="Choose a test case…"
            options={options} value={selectedId} onChange={(e) => { setSelectedId(e.target.value); setResult(null); setHistoryResult(null); setHistoryError(null) }} />
        </div>
        <div style={{ paddingBottom: 16 }}>
          <Button variant="primary" onClick={handleRun} loading={running} disabled={!selectedId}>
            {running ? 'Running…' : 'Run Test'}
          </Button>
        </div>
      </div>

      {selectedId && requiresHistory(ruleType) && (
        <div style={{ marginBottom: 16, padding: '14px 16px', background: 'var(--bg-secondary)', borderRadius: 8, border: '1px solid var(--border)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: historyResult || historyError ? 10 : 0 }}>
            <p style={{ margin: 0, flex: 1, fontSize: 13, color: 'var(--text-secondary)' }}>
              This rule depends on historical transactions. Generate matching history before running the test.
            </p>
            <Button variant="outline" onClick={handleGenerateHistory} loading={historyLoading} disabled={historyLoading}>
              Generate Required History
            </Button>
          </div>
          {historyResult && !historyResult.alreadyExists && (
            <p style={{ margin: 0, fontSize: 13, color: '#059669', fontWeight: 600 }}>
              Generated {historyResult.generatedCount} historical transactions for {historyResult.ruleType}
            </p>
          )}
          {historyResult?.alreadyExists && (
            <p style={{ margin: 0, fontSize: 13, color: '#2563eb' }}>i {historyResult.message}</p>
          )}
          {historyError && (
            <p style={{ margin: 0, fontSize: 13, color: '#dc2626' }}>{historyError}</p>
          )}
        </div>
      )}

      {result && (
        <div style={{
          padding: '16px 20px', borderRadius: 10, border: '1px solid',
          borderColor: result.status === 'PASSED' ? '#10b981' : '#ef4444',
          background: result.status === 'PASSED' ? '#f0fdf4' : '#fff5f5',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <PassFailBadge status={result.status} />
            {result.result && <span style={{ fontSize: 13 }}>Rule returned: <strong>{result.result}</strong></span>}
            <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--text-secondary)' }}>{result.durationMs}ms</span>
          </div>
          <FailureReasonBox reason={result.failureReason} />
          <RuleExplanationPanel explanation={getRuleExplanation(result.results?.[0])} />
          <ExecutionTracePanel trace={getExecutionTrace(result)} />
        </div>
      )}
    </div>
  )
}

export default ExecuteTestCasePanel
