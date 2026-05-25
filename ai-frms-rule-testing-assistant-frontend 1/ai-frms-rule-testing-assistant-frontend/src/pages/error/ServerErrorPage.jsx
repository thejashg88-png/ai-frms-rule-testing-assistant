import React from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../../components/common/Button'
import '../../styles/pages.css'

const ServerErrorPage = () => {
  const navigate = useNavigate()

  return (
    <div className="error-page">
      <div className="error-container">
        <h1 className="error-code">500</h1>
        <h2 className="error-title">Server Error</h2>
        <p className="error-message">
          Something went wrong on the server. Please try again in a moment or contact your system administrator.
        </p>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
          <Button variant="ghost" onClick={() => window.location.reload()}>Retry</Button>
          <Button variant="primary" onClick={() => navigate('/dashboard')}>Go to Dashboard</Button>
        </div>
      </div>
    </div>
  )
}

export default ServerErrorPage
