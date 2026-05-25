import React from 'react'
import './styles.css'

const Card = ({
  title,
  subtitle,
  children,
  footer,
  actions,
  className = '',
  noPadding = false,
}) => {
  return (
    <div className={`card ${className}`}>
      {(title || actions) && (
        <div className="card-header card-header-flex">
          <div>
            {title && <h3 className="card-heading">{title}</h3>}
            {subtitle && <p className="card-heading-subtitle">{subtitle}</p>}
          </div>
          {actions && <div className="card-actions">{actions}</div>}
        </div>
      )}
      <div className={noPadding ? '' : 'card-body'}>{children}</div>
      {footer && <div className="card-footer">{footer}</div>}
    </div>
  )
}

export default Card
