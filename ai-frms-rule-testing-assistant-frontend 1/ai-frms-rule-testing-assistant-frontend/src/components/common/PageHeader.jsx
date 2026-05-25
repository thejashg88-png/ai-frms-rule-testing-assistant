import React from 'react'
import './styles.css'

const PageHeader = ({
  title,
  subtitle,
  actions,
  breadcrumbs,
  className = '',
}) => {
  return (
    <div className={`page-header ${className}`}>
      <div className="page-header-top">
        <div className="page-header-content">
          <h1 className="page-title">{title}</h1>
          {subtitle && <p className="page-subtitle">{subtitle}</p>}
        </div>
        {actions && <div className="page-actions">{actions}</div>}
      </div>
      {breadcrumbs && <div className="page-breadcrumbs">{breadcrumbs}</div>}
    </div>
  )
}

export default PageHeader