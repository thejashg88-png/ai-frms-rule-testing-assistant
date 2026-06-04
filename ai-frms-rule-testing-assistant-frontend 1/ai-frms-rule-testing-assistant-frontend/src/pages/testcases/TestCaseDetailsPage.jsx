import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import TestCaseDetails from '../../components/testcases/TestCaseDetails'
import RuleExplanationPanel, { getRuleExplanation } from '../../components/common/RuleExplanationPanel'
import ExecutionTracePanel, { getExecutionTrace } from '../../components/common/ExecutionTracePanel'
import { useToast } from '../../hooks/useToast'
import testCaseService from '../../services/testCaseService'
import executionService from '../../services/executionService'
import transactionService from '../../services/transactionService'
import { requiresHistory } from '../../data/ruleTypes'

const TestCaseDetailsPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addToast } = useToast()
  const [testCase, setTestCase] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [running, setRunning] = useState(false)
  const [lastResult, setLastResult] = useState(null)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyResult, setHistoryResult] = useState(null)
  const [historyError, setHistoryError] = useState(null)

  useEffect(() => {
    setLastResult(null)   // clear stale result when navigating to a different test case
    setLoading(true)
    testCaseService.getById(id)
      .then(setTestCase)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  const handleRun = async () => {
    setLastResult(null)
    setRunning(true)
    try {
      const result = await executionService.runTestCase(id, testCase)
      setLastResult(result)
      const isPassed = result.status === 'PASSED' || result.normalizedStatus === 'PASSED'
      addToast(
        `Execution complete: ${result.status ?? result.normalizedStatus ?? 'UNKNOWN'}`,
        isPassed ? 'success' : 'error'
      )
    } catch (err) {
      addToast(err.message, 'error')
    } finally {
      setRunning(false)
    }
  }

  const getTestCaseRuleType = (tc) =>
    tc?.expectedResult?.expectedRuleType || tc?.ruleType || tc?.rule?.ruleType || null

  const handleGenerateHistory = async () => {
    const testCaseId = testCase?.testCaseId || testCase?.id || Number(id)
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

  if (loading) return <Loader message="Loading test case…" />
  if (error)   return <ErrorMessage title="Failed to load test case" message={error} />
  if (!testCase) return (
    <div>
      <PageHeader title="Test Case Details" subtitle="—" actions={
        <Button variant="ghost" onClick={() => navigate('/testcases')}>← Back</Button>
      } />
      <Card><p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: 14 }}>Test case details not available.</p></Card>
    </div>
  )

  const ruleType = getTestCaseRuleType(testCase)

  return (
    <div>
      <PageHeader
        title={testCase?.name ?? 'Test Case Details'}
        subtitle={`Rule: ${testCase?.ruleName ?? '—'}`}
        actions={
          <div style={{ display: 'flex', gap: 10 }}>
            <Button variant="ghost" onClick={() => navigate('/testcases')}>← Back</Button>
            <Button variant="outline" onClick={() => navigate(`/testcases/${id}/edit`)}>Edit</Button>
            <Button variant="primary" onClick={handleRun} loading={running}>
              {running ? 'Running…' : 'Run Test'}
            </Button>
          </div>
        }
      />

      {requiresHistory(ruleType) && (
        <div style={{ marginBottom: 20 }}>
          <Card title="Required History Data">
            <p style={{ margin: '0 0 12px', fontSize: 14, color: 'var(--text-secondary)' }}>
              This rule depends on historical transactions. Generate matching transaction history before running the test.
            </p>
            <Button variant="outline" onClick={handleGenerateHistory} loading={historyLoading}>
              Generate Required History
            </Button>
            {historyResult && !historyResult.alreadyExists && (
              <div style={{ marginTop: 12, padding: '10px 14px', background: 'rgba(16, 185, 129, 0.08)', borderRadius: 8, border: '1px solid #10b981' }}>
                <p style={{ margin: '0 0 6px', fontSize: 13, color: '#059669', fontWeight: 600 }}>
                  Generated {historyResult.generatedCount} historical transactions for {historyResult.ruleType}
                </p>
                {Array.isArray(historyResult.generatedTransactions) && historyResult.generatedTransactions.length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                    {historyResult.generatedTransactions.map((txn, i) => (
                      <div key={i} style={{ fontSize: 12, color: 'var(--text-secondary)', display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                        {txn.rrn && <span>RRN: {txn.rrn}</span>}
                        {txn.stan && <span>STAN: {txn.stan}</span>}
                        {txn.amount != null && <span>Amount: {txn.amount}</span>}
                        {txn.cardNumber && <span>Card: ****{String(txn.cardNumber).slice(-4)}</span>}
                        {txn.transactionTime && <span>Time: {txn.transactionTime}</span>}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
            {historyResult?.alreadyExists && (
              <div style={{ marginTop: 12, padding: '10px 14px', background: 'rgba(59, 130, 246, 0.08)', borderRadius: 8, border: '1px solid #3b82f6' }}>
                <p style={{ margin: 0, fontSize: 13, color: '#2563eb' }}>i {historyResult.message}</p>
              </div>
            )}
            {historyError && (
              <p style={{ margin: '10px 0 0', fontSize: 13, color: '#dc2626' }}>{historyError}</p>
            )}
          </Card>
        </div>
      )}

      {lastResult && (() => {
        const isPassed = lastResult.status === 'PASSED' || lastResult.normalizedStatus === 'PASSED' || lastResult.matched === true
        return (
          <div style={{
            marginBottom: 20, padding: '16px 20px', borderRadius: 10, border: '1px solid',
            borderColor: isPassed ? '#10b981' : '#ef4444',
            background:  isPassed ? '#f0fdf4' : '#fff5f5',
          }}>
            <strong style={{ color: isPassed ? '#16a34a' : '#dc2626' }}>
              {isPassed ? '✓ PASSED' : '✗ FAILED'}
            </strong>
            {' '}— Rule returned: <strong>{lastResult.result ?? '—'}</strong>
            {!isPassed && lastResult.failureReason && (
              <p style={{ margin: '8px 0 0', fontSize: 13, color: '#dc2626' }}>{lastResult.failureReason}</p>
            )}
            <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--text-secondary)' }}>
              {lastResult.durationMs != null ? `Duration: ${lastResult.durationMs}ms · ` : ''}
              {lastResult.executedAt ? new Date(lastResult.executedAt).toLocaleString() : ''}
            </p>
            <RuleExplanationPanel explanation={getRuleExplanation(lastResult.results?.[0])} />
            <ExecutionTracePanel trace={getExecutionTrace(lastResult)} />
          </div>
        )
      })()}

      <Card title="Test Case Details">
        <TestCaseDetails testCase={testCase} />
      </Card>
    </div>
  )
}

export default TestCaseDetailsPage
