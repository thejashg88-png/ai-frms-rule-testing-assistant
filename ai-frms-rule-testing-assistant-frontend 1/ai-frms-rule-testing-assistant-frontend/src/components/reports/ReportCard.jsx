import React from 'react'

const ReportCard = ({ title, value, subtitle, color = '#2563eb', icon }) => {
  return (
    <div style={{
      background: 'white', border: '1px solid var(--border)', borderRadius: 12,
      padding: '20px 24px', display: 'flex', alignItems: 'center', gap: 16,
    }}>
      {icon && (
        <div style={{
          width: 48, height: 48, borderRadius: 12, display: 'flex', alignItems: 'center',
          justifyContent: 'center', fontSize: 22,
          background: `${color}18`,
        }}>
          {icon}
        </div>
      )}
      <div>
        <p style={{ margin: 0, fontSize: 12, color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}>{title}</p>
        <p style={{ margin: '4px 0 2px', fontSize: 26, fontWeight: 700, color }}>{value ?? '—'}</p>
        {subtitle && <p style={{ margin: 0, fontSize: 12, color: 'var(--text-secondary)' }}>{subtitle}</p>}
      </div>
    </div>
  )
}

export default ReportCard
