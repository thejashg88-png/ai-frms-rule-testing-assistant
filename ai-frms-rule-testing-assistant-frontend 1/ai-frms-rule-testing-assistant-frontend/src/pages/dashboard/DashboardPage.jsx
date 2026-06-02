import React, { useState, useEffect } from 'react'
import PageHeader from '../../components/common/PageHeader'
import Card from '../../components/common/Card'
import Table from '../../components/common/Table'
import Badge from '../../components/common/Badge'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import DashboardSummaryCards from '../../components/dashboard/DashboardSummaryCards'
import dashboardService from '../../services/dashboardService'
import '../../styles/pages.css'

const STATUS_COLORS = {
  PASSED:  { bg: '#dcfce7', color: '#16a34a' },
  FAILED:  { bg: '#fee2e2', color: '#dc2626' },
  PENDING: { bg: '#fef9c3', color: '#ca8a04' },
}

const activityColumns = [
  { key: 'entityName', label: 'Test / Scenario Name' },
  {
    key: 'executionType',
    label: 'Type',
    width: '120px',
    render: (val) => (
      <Badge bgColor="#eff6ff" color="#2563eb" size="sm">
        {val?.replace('_', ' ') ?? '-'}
      </Badge>
    ),
  },
  {
    key: 'executedAt',
    label: 'Date',
    width: '110px',
    render: (val) => val ? val.split('T')[0] : '-',
  },
  {
    key: 'durationMs',
    label: 'Duration',
    width: '90px',
    render: (val) => val != null ? `${val}ms` : '-',
  },
  {
    key: 'status',
    label: 'Status',
    width: '100px',
    render: (val) => {
      const c = STATUS_COLORS[val] ?? { bg: '#f1f5f9', color: '#475569' }
      return <Badge bgColor={c.bg} color={c.color} size="sm">{val ?? '-'}</Badge>
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
  const [summary, setSummary] = useState(null)
  const [recentExecutions, setRecentExecutions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const [summaryData, executionsData] = await Promise.all([
        dashboardService.getSummary(),
        dashboardService.getRecentExecutions(7),
      ])
      console.log('[Dashboard Summary]', summaryData)
      setSummary(summaryData)
      setRecentExecutions(executionsData)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  if (loading) return <Loader message="Loading dashboard..." />
  if (error) return <ErrorMessage title="Failed to load dashboard" message={error} onRetry={load} />

  const stats = {
    totalRules:      summary?.totalRules      ?? 0,
    activeRules:     summary?.activeRules     ?? 0,
    totalScenarios:  summary?.totalScenarios  ?? 0,
    totalTests:      summary?.totalTestCases  ?? 0,
    successRate:     Math.round(summary?.passRate ?? 0),
  }

  const totalExec   = summary?.totalExecutions   ?? 0
  const passedExec  = summary?.passedExecutions  ?? 0
  const failedExec  = summary?.failedExecutions  ?? 0

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
          subtitle="Latest rule test runs"
          noPadding
        >
          <Table
            columns={activityColumns}
            data={recentExecutions}
            emptyMessage="No executions yet."
          />
        </Card>

        {/* ── Side stats ── */}
        <div className="dashboard-side-stack">
          <Card title="Execution Summary">
            <div className="stat-list">
              <StatRow label="Total Executions" value={totalExec}   tone="blue" />
              <StatRow label="Passed"           value={passedExec}  tone="positive" />
              <StatRow label="Failed"           value={failedExec}  tone="negative" />
              <StatRow label="Success Rate"     value={`${stats.successRate}%`} tone="positive" />
            </div>
          </Card>

          <Card title="Rules Overview">
            <div className="stat-list">
              <StatRow label="Total Rules" value={stats.totalRules}  tone="blue" />
              <StatRow label="Active"      value={stats.activeRules} tone="positive" />
              <StatRow label="Inactive"    value={stats.totalRules - stats.activeRules} />
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
