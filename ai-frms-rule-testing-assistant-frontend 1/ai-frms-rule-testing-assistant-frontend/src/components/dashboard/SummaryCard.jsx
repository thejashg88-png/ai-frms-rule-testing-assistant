import React from 'react'
import './dashboard.css'

const SummaryCard = ({ title, value, subtitle, icon: Icon, color = 'blue', trend }) => {
  return (
    <div className={`summary-card summary-card-${color}`}>
      <div className="summary-card-header">
        {Icon && (
          <div className={`summary-card-icon summary-card-icon-${color}`}>
            <Icon size={20} />
          </div>
        )}
        <div className="summary-card-meta">
          <p className="card-title">{title}</p>
          {subtitle && <p className="card-subtitle">{subtitle}</p>}
        </div>
      </div>
      <div className="card-content">
        <div className="card-value">{value}</div>
        {trend !== undefined && (
          <div className={`card-trend trend-${trend >= 0 ? 'up' : 'down'}`}>
            {trend >= 0 ? '↑' : '↓'} {Math.abs(trend)}%
          </div>
        )}
      </div>
    </div>
  )
}

export default SummaryCard
