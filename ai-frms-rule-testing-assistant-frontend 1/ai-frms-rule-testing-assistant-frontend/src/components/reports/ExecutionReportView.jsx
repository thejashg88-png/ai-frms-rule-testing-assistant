import React from 'react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import ReportCard from './ReportCard'

const ExecutionReportView = ({ report }) => {
  if (!report) return null

  const trendData = report.trend ?? []

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <ReportCard title="Total Executions" value={report.totalExecutions} icon="🔄" color="#2563eb" />
        <ReportCard title="Passed" value={report.passed} icon="✓" color="#10b981" />
        <ReportCard title="Failed" value={report.failed} icon="✗" color="#ef4444" />
        <ReportCard title="Pass Rate" value={`${(report.passRate ?? 0).toFixed(1)}%`} icon="%" color="#7c3aed" subtitle="of all executions" />
      </div>

      {trendData.length > 0 && (
        <div style={{ background: 'white', border: '1px solid var(--border)', borderRadius: 12, padding: '20px 24px' }}>
          <h4 style={{ margin: '0 0 16px', fontSize: 15, fontWeight: 600 }}>Execution Trend</h4>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={trendData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Legend />
              <Bar dataKey="passed" stackId="a" fill="#10b981" name="Passed" radius={[0, 0, 0, 0]} />
              <Bar dataKey="failed" stackId="a" fill="#ef4444" name="Failed" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}

export default ExecutionReportView
