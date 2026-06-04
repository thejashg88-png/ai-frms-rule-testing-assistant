import React from 'react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from 'recharts'
import './dashboard.css'

const RULE_TYPE_SHORT = {
  HIGH_FREQ_TXN:   'High Freq',
  SEQUENTIAL_TXN:  'Sequential',
  STRUCTURING:     'Structuring',
  UNUSUAL_AMT:     'Unusual Amt',
  INCONSISTENT_MCC:'Incon. MCC',
  ROUND_AMT_TXN:   'Round Amt',
  TXN_VELOCITY:    'Velocity',
  SINGLE_LARGE_TX: 'Single Large',
}

const shortenLabel = (label) => RULE_TYPE_SHORT[label] || label

const mapToChartData = (mapObj) =>
  Object.entries(mapObj || {}).map(([label, value]) => ({
    label: shortenLabel(label),
    value: Number(value || 0),
  }))

const isEmpty = (data) =>
  !data || data.length === 0 || data.every((d) => d.value === 0)

const PASS_FAIL_COLORS  = { PASSED: '#10b981', FAILED: '#ef4444' }
const ACTION_COLORS     = { ACCEPT: '#10b981', MONITOR: '#f59e0b', REJECT: '#ef4444' }
const STATUS_COLORS     = { APPROVED: '#10b981', DECLINED: '#ef4444', PENDING: '#f59e0b' }

const getBarColor = (label, colorMap) => colorMap[label] ?? '#6366f1'

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div style={{
      background: 'var(--bg-primary)',
      border: '1px solid var(--border)',
      borderRadius: 6,
      padding: '8px 12px',
      fontSize: 12,
      color: 'var(--text-primary)',
      boxShadow: 'var(--shadow)',
    }}>
      <p style={{ margin: 0, fontWeight: 600 }}>{label}</p>
      <p style={{ margin: '2px 0 0', color: payload[0].fill }}>{payload[0].value}</p>
    </div>
  )
}

const ChartCard = ({ title, children, empty }) => (
  <div className="dashboard-chart-card">
    <p className="dashboard-chart-title">{title}</p>
    {empty
      ? (
        <div className="dashboard-chart-empty">No data available yet</div>
      )
      : children
    }
  </div>
)

const BarChartBlock = ({ data, colorMap }) => (
  <ResponsiveContainer width="100%" height={180}>
    <BarChart data={data} margin={{ top: 4, right: 8, left: -20, bottom: 4 }}>
      <XAxis
        dataKey="label"
        tick={{ fontSize: 11, fill: 'var(--text-secondary)' }}
        axisLine={false}
        tickLine={false}
      />
      <YAxis
        tick={{ fontSize: 11, fill: 'var(--text-secondary)' }}
        axisLine={false}
        tickLine={false}
        allowDecimals={false}
      />
      <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(99,102,241,0.06)' }} />
      <Bar dataKey="value" radius={[4, 4, 0, 0]} maxBarSize={48}>
        {data.map((entry, i) => (
          <Cell key={i} fill={getBarColor(entry.label, colorMap)} />
        ))}
      </Bar>
    </BarChart>
  </ResponsiveContainer>
)

const DashboardCharts = ({ summary }) => {
  const passFailData   = mapToChartData(summary?.passFailDistribution)
  const execByRuleData = mapToChartData(summary?.executionsByRuleType)
  const riskActionData = mapToChartData(summary?.riskActionDistribution)
  const txnStatusData  = mapToChartData(summary?.transactionStatusDistribution)

  return (
    <div className="dashboard-charts-grid">
      <ChartCard title="Pass vs Failed" empty={isEmpty(passFailData)}>
        <BarChartBlock data={passFailData} colorMap={PASS_FAIL_COLORS} />
      </ChartCard>

      <ChartCard title="Executions by Rule Type" empty={isEmpty(execByRuleData)}>
        <BarChartBlock data={execByRuleData} colorMap={{}} />
      </ChartCard>

      <ChartCard title="Risk Action Distribution" empty={isEmpty(riskActionData)}>
        <BarChartBlock data={riskActionData} colorMap={ACTION_COLORS} />
      </ChartCard>

      <ChartCard title="Transaction Status Distribution" empty={isEmpty(txnStatusData)}>
        <BarChartBlock data={txnStatusData} colorMap={STATUS_COLORS} />
      </ChartCard>
    </div>
  )
}

export default DashboardCharts
