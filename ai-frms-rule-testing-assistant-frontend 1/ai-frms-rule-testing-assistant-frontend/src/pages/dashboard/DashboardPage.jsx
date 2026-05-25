import React, { useState, useEffect } from 'react'
import PageHeader from '../../components/common/PageHeader'
import Card from '../../components/common/Card'
import Table from '../../components/common/Table'
import Badge from '../../components/common/Badge'
import Loader from '../../components/common/Loader'
import DashboardSummaryCards from '../../components/dashboard/DashboardSummaryCards'
import '../../styles/pages.css'

const MOCK_ACTIVITY = [
  { id: 1, ruleName: 'Credit Limit Check',     ruleType: 'CREDIT',   status: 'PASSED',  executedBy: 'admin',   date: '2025-05-24' },
  { id: 2, ruleName: 'High Value TX Alert',    ruleType: 'AMOUNT',   status: 'FAILED',  executedBy: 'tester1', date: '2025-05-24' },
  { id: 3, ruleName: 'Card Velocity Rule',     ruleType: 'VELOCITY', status: 'PASSED',  executedBy: 'admin',   date: '2025-05-23' },
  { id: 4, ruleName: 'Geo Mismatch Detect',    ruleType: 'GEO',      status: 'PASSED',  executedBy: 'tester2', date: '2025-05-23' },
  { id: 5, ruleName: 'Duplicate TXN Check',    ruleType: 'FRAUD',    status: 'FAILED',  executedBy: 'admin',   date: '2025-05-22' },
  { id: 6, ruleName: 'Foreign Card Rule',      ruleType: 'CARD',     status: 'PASSED',  executedBy: 'tester1', date: '2025-05-22' },
  { id: 7, ruleName: 'Frequency Limit Rule',   ruleType: 'VELOCITY', status: 'PENDING', executedBy: 'admin',   date: '2025-05-21' },
]

const STATUS_COLORS = {
  PASSED:  { bg: '#dcfce7', color: '#16a34a' },
  FAILED:  { bg: '#fee2e2', color: '#dc2626' },
  PENDING: { bg: '#fef9c3', color: '#ca8a04' },
}

const activityColumns = [
  { key: 'ruleName', label: 'Rule Name' },
  { key: 'ruleType', label: 'Type', width: '110px',
    render: (val) => (
      <Badge bgColor="#eff6ff" color="#2563eb" size="sm">{val}</Badge>
    ),
  },
  { key: 'executedBy', label: 'By', width: '100px' },
  { key: 'date', label: 'Date', width: '110px' },
  { key: 'status', label: 'Status', width: '100px',
    render: (val) => {
      const c = STATUS_COLORS[val] ?? {}
      return <Badge bgColor={c.bg} color={c.color} size="sm">{val}</Badge>
    },
  },
]

const StatRow = ({ label, value, tone }) => (
  <div className="stat-row">
    <span className="stat-row-label">{label}</span>
    <span className={`stat-row-value ${tone ?? ''}`}>{value}</span>
  </div>
)

const DashboardPage = () => {
  const [loading, setLoading] = useState(true)
  const stats = {
    totalRules: 45,
    activeRules: 38,
    totalTests: 156,
    passedTests: 142,
    failedTests: 14,
    successRate: 91,
  }

  useEffect(() => {
    const t = setTimeout(() => setLoading(false), 400)
    return () => clearTimeout(t)
  }, [])

  if (loading) return <Loader message="Loading dashboard..." />

  return (
    <div className="dashboard-page">
      <PageHeader
        title="Dashboard"
        subtitle="AI FRMS Rule Testing Assistant — overview"
      />

      <DashboardSummaryCards stats={stats} />

      <div className="dashboard-body-grid">
        {/* ── Recent Activity (main column) ── */}
        <Card
          title="Recent Test Executions"
          subtitle="Latest rule test runs across all users"
          noPadding
        >
          <Table
            columns={activityColumns}
            data={MOCK_ACTIVITY}
            emptyMessage="No executions yet."
          />
        </Card>

        {/* ── Side stats ── */}
        <div className="dashboard-side-stack">
          <Card title="Execution Summary">
            <div className="stat-list">
              <StatRow label="Total Executions" value={stats.totalTests} tone="blue" />
              <StatRow label="Passed"           value={stats.passedTests} tone="positive" />
              <StatRow label="Failed"           value={stats.failedTests} tone="negative" />
              <StatRow label="Success Rate"     value={`${stats.successRate}%`} tone="positive" />
            </div>
          </Card>

          <Card title="Rules Overview">
            <div className="stat-list">
              <StatRow label="Total Rules"    value={stats.totalRules} tone="blue" />
              <StatRow label="Active"         value={stats.activeRules} tone="positive" />
              <StatRow label="Inactive"       value={stats.totalRules - stats.activeRules} />
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
