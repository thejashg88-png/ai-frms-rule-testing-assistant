import React, { useState } from 'react'
import PageHeader from '../../components/common/PageHeader'
import Card from '../../components/common/Card'
import AiRuleExplanation from '../../components/ai/AiRuleExplanation'
import AiGenerateTestCases from '../../components/ai/AiGenerateTestCases'
import AiFailureAnalysis from '../../components/ai/AiFailureAnalysis'
import AiTransactionGenerator from '../../components/ai/AiTransactionGenerator'
import AiChat from '../../components/ai/AiChat'

// Five AI tabs — each tab renders a standalone component that calls aiService independently.
// explain  → AiRuleExplanation  — explain what a rule does and its risk level
// generate → AiGenerateTestCases — suggest test cases for a rule using AI
// analyze  → AiFailureAnalysis  — root cause analysis for a failed execution
// txngen   → AiTransactionGenerator — generate realistic dummy transaction data
// chat     → AiChat              — free-form Q&A about rules, executions, and testing
const TABS = [
  { id: 'explain',    label: 'Explain Rule' },
  { id: 'generate',  label: 'Generate Test Cases' },
  { id: 'analyze',   label: 'Failure Analysis' },
  { id: 'txngen',    label: 'Transaction Generator' },
  { id: 'chat',      label: 'AI Chat' },
]

const AiAssistantPage = () => {
  const [activeTab, setActiveTab] = useState('explain')

  const tabStyle = (id) => ({
    padding: '8px 18px',
    border: 'none',
    cursor: 'pointer',
    fontWeight: 500,
    fontSize: 14,
    borderRadius: 8,
    background: activeTab === id ? 'var(--primary)' : 'transparent',
    color: activeTab === id ? 'white' : 'var(--text-secondary)',
    transition: 'all 0.2s',
  })

  return (
    <div>
      <PageHeader
        title="AI Assistant"
        subtitle="Use AI to explain rules, generate test cases, and analyze failures"
      />

      <div style={{ display: 'flex', gap: 4, marginBottom: 20, background: 'var(--bg-secondary)', padding: 4, borderRadius: 10, width: 'fit-content', border: '1px solid var(--border)', flexWrap: 'wrap' }}>
        {TABS.map((t) => (
          <button key={t.id} style={tabStyle(t.id)} onClick={() => setActiveTab(t.id)}>
            {t.label}
          </button>
        ))}
      </div>

      {activeTab === 'explain' && (
        <Card title="Rule Explanation" subtitle="Select a rule to get an AI-powered explanation of its logic and purpose">
          <AiRuleExplanation />
        </Card>
      )}

      {activeTab === 'generate' && (
        <Card title="Generate Test Cases" subtitle="Select a rule and let AI suggest relevant test cases">
          <AiGenerateTestCases />
        </Card>
      )}

      {activeTab === 'analyze' && (
        <Card title="Failure Analysis" subtitle="Select a failed execution for AI-powered root cause analysis">
          <AiFailureAnalysis />
        </Card>
      )}

      {activeTab === 'txngen' && (
        <Card title="Transaction Generator" subtitle="Generate realistic dummy transactions for testing">
          <AiTransactionGenerator />
        </Card>
      )}

      {activeTab === 'chat' && (
        <Card title="AI Chat" subtitle="Ask questions about fraud rules, test cases, executions, or debugging">
          <AiChat />
        </Card>
      )}
    </div>
  )
}

export default AiAssistantPage
