import React from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../../components/common/Button'
import '../../styles/pages.css'

const NotFoundPage = () => {
  const navigate = useNavigate()

  return (
    <div className="error-page">
      <div className="error-container">
        <h1 className="error-code">404</h1>
        <h2 className="error-title">Page Not Found</h2>
        <p className="error-message">
          The page you're looking for doesn't exist or has been moved.
        </p>
        <Button variant="primary" onClick={() => navigate('/dashboard')}>
          Go to Dashboard
        </Button>
      </div>
    </div>
  )
}

export default NotFoundPage
