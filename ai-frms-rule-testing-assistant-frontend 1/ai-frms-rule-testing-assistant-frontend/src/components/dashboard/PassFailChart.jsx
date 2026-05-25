import React from 'react'
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts'

const COLORS = ['#10b981', '#ef4444', '#6b7280']

const PassFailChart = ({ passed = 0, failed = 0, skipped = 0 }) => {
  const data = [
    { name: 'Passed', value: passed },
    { name: 'Failed', value: failed },
    { name: 'Skipped', value: skipped },
  ].filter((d) => d.value > 0)

  if (data.length === 0) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 200, color: 'var(--text-secondary)', fontSize: 14 }}>
        No execution data yet
      </div>
    )
  }

  return (
    <ResponsiveContainer width="100%" height={200}>
      <PieChart>
        <Pie data={data} cx="50%" cy="50%" innerRadius={55} outerRadius={80} paddingAngle={3} dataKey="value">
          {data.map((_, index) => (
            <Cell key={index} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip formatter={(value) => [value, 'Executions']} />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  )
}

export default PassFailChart
