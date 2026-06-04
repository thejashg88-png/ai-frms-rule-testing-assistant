import React, { useState, useEffect } from 'react'
import Select from '../common/Select'
import Button from '../common/Button'
import scenarioService from '../../services/scenarioService'
import executionService from '../../services/executionService'
import { useToast } from '../../hooks/useToast'
import PassFailBadge from './PassFailBadge'
import FailureReasonBox from './FailureReasonBox'
import RuleExplanationPanel, { getRuleExplanation } from '../common/RuleExplanationPanel'
import ExecutionTracePanel, { getExecutionTrace } from '../common/ExecutionTracePanel'

const normalizeStatus = (val) => {
  const v = String(val || '').toUpperCase()
  if (v === 'PASS' || v === 'PASSED' || v === 'SUCCESS') return 'PASSED'
  if (v === 'FAIL' || v === 'FAILED' || v === 'ERROR')   return 'FAILED'
  return v || 'UNKNOWN'
}

const ExecuteScenarioPanel = ({ onExecuted }) => {
  const { addToast } = useToast()
  const [scenarios, setScenarios] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [running, setRunning] = useState(false)
  const [result, setResult] = useState(null)

  useEffect(() => {
    scenarioService.getAll().then(setScenarios).catch(() => {})
  }, [])

  const options = scenarios.map((s) => ({ value: String(s.id), label: s.name }))

  const handleRun = async () => {
    if (!selectedId) return
    setRunning(true); setResult(null)
    try {
      const sc = scenarios.find((s) => s.id === Number(selectedId))
      const execution = await executionService.runScenario(selectedId, sc)
      setResult(execution)
      addToast(`Scenario ${execution.status}`, execution.status === 'PASSED' ? 'success' : 'error')
      onExecuted?.(execution)
    } catch (err) { addToast(err.message, 'error') }
    finally { setRunning(false) }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 16 }}>
        <div style={{ flex: 1 }}>
          <Select label="Select Scenario" name="scenario" placeholder="Choose a scenario…"
            options={options} value={selectedId} onChange={(e) => { setSelectedId(e.target.value); setResult(null) }} />
        </div>
        <div style={{ paddingBottom: 16 }}>
          <Button variant="primary" onClick={handleRun} loading={running} disabled={!selectedId}>
            {running ? 'Running…' : 'Run Scenario'}
          </Button>
        </div>
      </div>

      {result && (
        <div style={{
          padding: '16px 20px', borderRadius: 10, border: '1px solid',
          borderColor: result.status === 'PASSED' ? '#10b981' : '#ef4444',
          background: result.status === 'PASSED' ? '#f0fdf4' : '#fff5f5',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <PassFailBadge status={result.status} />
            <span style={{ fontSize: 13 }}>
              {result.status === 'PASSED' ? 'All test cases passed' : 'Some test cases failed'}
            </span>
            <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--text-secondary)' }}>{result.durationMs}ms</span>
          </div>
          <FailureReasonBox reason={result.failureReason} />

          {/* Per-test-case breakdown with individual rule explanations */}
          {Array.isArray(result.results) && result.results.length > 0 && (
            <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
              {result.results.map((r, i) => {
                const tcName    = r.testCaseName || r.name || `Test Case ${i + 1}`
                const tcStatus  = normalizeStatus(r.resultStatus || r.status)
                const explanation = getRuleExplanation(r)
                return (
                  <div key={i} style={{
                    padding: '10px 14px',
                    border: '1px solid var(--border)',
                    borderRadius: 8,
                    background: 'var(--bg-primary)',
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                      <PassFailBadge status={tcStatus} />
                      <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)' }}>{tcName}</span>
                      {r.actualAction && (
                        <span style={{ fontSize: 12, color: 'var(--text-secondary)', marginLeft: 4 }}>
                          → {r.actualAction}
                        </span>
                      )}
                    </div>
                    {r.failureReason && <FailureReasonBox reason={r.failureReason} />}
                    {explanation && <RuleExplanationPanel explanation={explanation} />}
                    <ExecutionTracePanel trace={getExecutionTrace(r)} />
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default ExecuteScenarioPanel
