import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Card from '../../components/common/Card'
import ExecuteTestCasePanel from '../../components/executions/ExecuteTestCasePanel'
import ExecuteScenarioPanel from '../../components/executions/ExecuteScenarioPanel'

// Two execution modes:
// - Test Case: runs a single test case against the rule engine and returns one result.
// - Scenario:  runs ALL ACTIVE test cases in the selected scenario in one backend call.
const RunExecutionPage = () => {
  const navigate = useNavigate()
  const [tab, setTab] = useState('testcase')
  // Stores the most recent execution result so the "View Last Result" shortcut button works.
  const [lastExecution, setLastExecution] = useState(null)

  const tabStyle = (t) => ({
    padding: '8px 20px',
    borderRadius: 8,
    border: 'none',
    cursor: 'pointer',
    fontWeight: 500,
    fontSize: 14,
    background: tab === t ? 'var(--primary)' : 'transparent',
    color: tab === t ? 'white' : 'var(--text-secondary)',
    transition: 'all 0.2s',
  })

  return (
    <div>
      <PageHeader
        title="Run Execution"
        subtitle="Execute a test case or scenario against the FRMS rule engine"
        actions={
          <div style={{ display: 'flex', gap: 10 }}>
            <Button variant="ghost" onClick={() => navigate('/executions')}>← Back to Executions</Button>
            {lastExecution && (
              <Button variant="outline" onClick={() => navigate(`/executions/${lastExecution.id}`)}>
                View Last Result
              </Button>
            )}
          </div>
        }
      />

      <div style={{ display: 'flex', gap: 8, marginBottom: 20, background: 'var(--bg-secondary)', padding: 4, borderRadius: 10, width: 'fit-content', border: '1px solid var(--border)' }}>
        <button style={tabStyle('testcase')} onClick={() => setTab('testcase')}>Test Case</button>
        <button style={tabStyle('scenario')} onClick={() => setTab('scenario')}>Scenario</button>
      </div>

      <Card title={tab === 'testcase' ? 'Execute Test Case' : 'Execute Scenario'}
        subtitle={tab === 'testcase'
          ? 'Select a test case and run it against the rule engine'
          : 'Select a scenario to run all its test cases'}>
        {tab === 'testcase'
          ? <ExecuteTestCasePanel onExecuted={setLastExecution} />
          : <ExecuteScenarioPanel onExecuted={setLastExecution} />
        }
      </Card>
    </div>
  )
}

export default RunExecutionPage
