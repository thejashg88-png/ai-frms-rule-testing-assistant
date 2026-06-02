import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import TestCaseDetails from '../../components/testcases/TestCaseDetails'
import { useToast } from '../../hooks/useToast'
import testCaseService from '../../services/testCaseService'
import executionService from '../../services/executionService'

const TestCaseDetailsPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addToast } = useToast()
  const [testCase, setTestCase] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [running, setRunning] = useState(false)
  const [lastResult, setLastResult] = useState(null)

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
