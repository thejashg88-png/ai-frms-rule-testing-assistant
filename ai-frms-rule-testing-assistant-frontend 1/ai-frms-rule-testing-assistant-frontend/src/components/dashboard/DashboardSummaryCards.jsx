import React from 'react'
import {
  Shield, ShieldCheck, Layers, TestTube2, Activity,
  CheckCircle, XCircle, TrendingUp, AlertTriangle, Zap,
} from 'lucide-react'
import SummaryCard from './SummaryCard'
import './dashboard.css'

const RULE_TYPE_LABELS = {
  HIGH_FREQ_TXN:   'High Frequency Transaction',
  SEQUENTIAL_TXN:  'Sequential Transaction',
  STRUCTURING:     'Structuring',
  UNUSUAL_AMT:     'Unusual Amount',
  INCONSISTENT_MCC:'Inconsistent MCC',
  ROUND_AMT_TXN:   'Round Amount Transaction',
  TXN_VELOCITY:    'Transaction Velocity',
  SINGLE_LARGE_TX: 'Single Large Transaction',
}

const friendlyRuleType = (val) => {
  if (!val || val === 'N/A') return 'N/A'
  return RULE_TYPE_LABELS[val] || val.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}

const formatPassRate = (rate) => {
  if (rate == null) return '0%'
  const n = Number(rate)
  return n === 0 ? '0%' : `${n.toFixed(2)}%`
}

const DashboardSummaryCards = ({ summary }) => {
  const cards = [
    {
      title: 'Total Rules',
      value: summary?.totalRules ?? 0,
      subtitle: 'Defined in system',
      color: 'blue',
      icon: Shield,
    },
    {
      title: 'Active Rules',
      value: summary?.activeRules ?? 0,
      subtitle: 'Running now',
      color: 'green',
      icon: ShieldCheck,
    },
    {
      title: 'Total Scenarios',
      value: summary?.totalScenarios ?? 0,
      subtitle: 'Test scenario groups',
      color: 'teal',
      icon: Layers,
    },
    {
      title: 'Total Test Cases',
      value: summary?.totalTestCases ?? 0,
      subtitle: 'Total created',
      color: 'purple',
      icon: TestTube2,
    },
    {
      title: 'Total Executions',
      value: summary?.totalExecutions ?? 0,
      subtitle: 'All test runs',
      color: 'orange',
      icon: Activity,
    },
    {
      title: 'Passed',
      value: summary?.passedExecutions ?? 0,
      subtitle: 'Executions passed',
      color: 'green',
      icon: CheckCircle,
    },
    {
      title: 'Failed',
      value: summary?.failedExecutions ?? 0,
      subtitle: 'Executions failed',
      color: 'red',
      icon: XCircle,
    },
    {
      title: 'Pass Rate',
      value: formatPassRate(summary?.passRate),
      subtitle: 'Overall success rate',
      color: 'teal',
      icon: TrendingUp,
    },
    {
      title: 'Most Failed Rule',
      value: friendlyRuleType(summary?.mostFailedRuleType),
      subtitle: 'Highest failure count',
      color: 'orange',
      icon: AlertTriangle,
      compact: true,
    },
    {
      title: 'Most Triggered Rule',
      value: summary?.mostTriggeredRule || 'N/A',
      subtitle: 'Highest execution count',
      color: 'purple',
      icon: Zap,
      compact: true,
    },
  ]

  return (
    <div className="summary-cards-grid">
      {cards.map((card, i) => (
        <SummaryCard key={i} {...card} />
      ))}
    </div>
  )
}

export default DashboardSummaryCards
