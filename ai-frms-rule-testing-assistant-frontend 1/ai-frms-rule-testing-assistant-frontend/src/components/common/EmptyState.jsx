import React from 'react'
import Button from './Button'

const EmptyState = ({
  icon = '📭',
  title = 'Nothing here yet',
  description,
  actionLabel,
  onAction,
}) => {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '60px 24px',
      gap: '12px',
      textAlign: 'center',
    }}>
      <span style={{ fontSize: 48, lineHeight: 1 }}>{icon}</span>
      <h3 style={{ margin: 0, fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>
        {title}
      </h3>
      {description && (
        <p style={{ margin: 0, fontSize: 14, color: 'var(--text-secondary)', maxWidth: 360, lineHeight: 1.6 }}>
          {description}
        </p>
      )}
      {actionLabel && onAction && (
        <Button variant="primary" onClick={onAction} style={{ marginTop: 8 }}>
          {actionLabel}
        </Button>
      )}
    </div>
  )
}

export default EmptyState
