import React from 'react'
import Loader from '../common/Loader'

const AiResponseBox = ({ loading, content, error }) => {
  return (
    <div style={{
      background: '#f8fafc',
      border: '1px solid var(--border)',
      borderRadius: 10,
      padding: 20,
      minHeight: 120,
    }}>
      {loading && <Loader message="AI is thinking…" />}
      {error && (
        <p style={{ margin: 0, color: 'var(--danger)', fontSize: 14 }}>
          {error}
        </p>
      )}
      {!loading && !error && !content && (
        <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: 14 }}>
          AI response will appear here.
        </p>
      )}
      {!loading && !error && content && (
        <div style={{ fontSize: 14, color: 'var(--text-primary)', lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
          {content}
        </div>
      )}
    </div>
  )
}

export default AiResponseBox
