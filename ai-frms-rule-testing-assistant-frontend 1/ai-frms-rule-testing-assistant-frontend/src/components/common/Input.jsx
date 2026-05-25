import React from 'react'
import './styles.css'

const Input = React.forwardRef(({
  label,
  error,
  helperText,
  type = 'text',
  placeholder = '',
  disabled = false,
  required = false,
  className = '',
  ...props
}, ref) => {
  return (
    <div className="input-wrapper">
      {label && (
        <label className="input-label">
          {label}
          {required && <span className="required">*</span>}
        </label>
      )}
      <input
        ref={ref}
        type={type}
        placeholder={placeholder}
        disabled={disabled}
        className={`input ${error ? 'input-error' : ''} ${className}`}
        {...props}
      />
      {error && <span className="error-text">{error}</span>}
      {helperText && !error && <span className="helper-text">{helperText}</span>}
    </div>
  )
})

Input.displayName = 'Input'

export default Input