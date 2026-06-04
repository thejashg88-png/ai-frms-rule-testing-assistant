import React from 'react'
import Loader from '../common/Loader'

const AiResponseBox = ({ loading, content, error }) => {
  return (
    <div className="ai-response-box">
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
