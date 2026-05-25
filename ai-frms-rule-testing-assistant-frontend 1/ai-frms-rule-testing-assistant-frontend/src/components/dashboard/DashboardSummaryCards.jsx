import React from 'react'
import { Shield, ShieldCheck, TestTube2, TrendingUp } from 'lucide-react'
import SummaryCard from './SummaryCard'
import './dashboard.css'

const DashboardSummaryCards = ({ stats }) => {
  const cards = [
    {
      title: 'Total Rules',
      value: stats.totalRules,
      subtitle: 'Defined in system',
      color: 'blue',
      icon: Shield,
      trend: 5,
    },
    {
      title: 'Active Rules',
      value: stats.activeRules,
      subtitle: 'Running now',
      color: 'green',
      icon: ShieldCheck,
      trend: 3,
    },
    {
      title: 'Test Cases',
      value: stats.totalTests,
      subtitle: 'Total created',
      color: 'purple',
      icon: TestTube2,
      trend: 8,
    },
    {
      title: 'Success Rate',
      value: `${stats.successRate}%`,
      subtitle: 'Executions passed',
      color: 'orange',
      icon: TrendingUp,
      trend: 2,
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
