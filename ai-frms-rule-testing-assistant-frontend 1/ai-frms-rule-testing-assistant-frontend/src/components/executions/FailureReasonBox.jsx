import React from 'react'

const FailureReasonBox = ({ reason }) => {
  if (!reason) return null
  return (
    <div style={{
      marginTop: 12, padding: '12px 16px', background: '#fff5f5',
      border: '1px solid #fca5a5', borderRadius: 8,
    }}>
      <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: '#dc2626', marginBottom: 4 }}>Failure Reason</p>
      <p style={{ margin: 0, fontSize: 13, color: '#7f1d1d', lineHeight: 1.6 }}>{reason}</p>
    </div>
  )
}

export default FailureReasonBox
