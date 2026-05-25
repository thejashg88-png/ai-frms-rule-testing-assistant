import React from 'react'
import './styles.css'

const Badge = ({
  children,
  variant = 'default',
  color,
  bgColor,
  size = 'md',
  className = '',
}) => {
  const style = {
    color: color,
    backgroundColor: bgColor,
  }

  return (
    <span
      className={`badge badge-${variant} badge-${size} ${className}`}
      style={color || bgColor ? style : undefined}
    >
      {children}
    </span>
  )
}

export default Badge