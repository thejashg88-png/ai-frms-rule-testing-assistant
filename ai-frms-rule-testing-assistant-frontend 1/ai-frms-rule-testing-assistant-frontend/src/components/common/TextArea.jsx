import React from 'react'
import './styles.css'

const TextArea = React.forwardRef(({
  label,
  error,
  helperText,
  placeholder = '',
  disabled = false,
  required = false,
  rows = 4,
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
      <textarea
        ref={ref}
        rows={rows}
        placeholder={placeholder}
        disabled={disabled}
        className={`input ${error ? 'input-error' : ''} ${className}`}
        style={{ resize: 'vertical', minHeight: `${rows * 24}px` }}
        {...props}
      />
      {error && <span className="error-text">{error}</span>}
      {helperText && !error && <span className="helper-text">{helperText}</span>}
    </div>
  )
})

TextArea.displayName = 'TextArea'

export default TextArea
