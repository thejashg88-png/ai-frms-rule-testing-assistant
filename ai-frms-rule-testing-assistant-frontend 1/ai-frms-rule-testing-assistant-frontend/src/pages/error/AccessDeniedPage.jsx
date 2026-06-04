import React from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../../components/common/Button'
import '../../styles/pages.css'

const AccessDeniedPage = () => {
  const navigate = useNavigate()

  return (
    <div className="error-page">
      <div className="error-container">
        <h1 className="error-code">403</h1>
        <h2 className="error-title">Access Denied</h2>
        <p className="error-message">
          You do not have permission to access this page.
          Contact your administrator if you believe this is a mistake.
        </p>
        <Button variant="primary" onClick={() => navigate('/dashboard')}>
          Go to Dashboard
        </Button>
      </div>
    </div>
  )
}

export default AccessDeniedPage
