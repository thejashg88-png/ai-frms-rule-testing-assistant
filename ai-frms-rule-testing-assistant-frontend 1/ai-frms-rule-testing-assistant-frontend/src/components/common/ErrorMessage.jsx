import React from 'react'
import './styles.css'
import Button from './Button'

const ErrorMessage = ({
  title = 'Something went wrong',
  message,
  onRetry,
}) => {
  return (
    <div className="error-message-box">
      <div className="error-message-icon">⚠</div>
      <h3 className="error-message-title">{title}</h3>
      {message && <p className="error-message-text">{message}</p>}
      {onRetry && (
        <Button variant="primary" size="sm" onClick={onRetry}>
          Try Again
        </Button>
      )}
    </div>
  )
}

export default ErrorMessage
