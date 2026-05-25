import React from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import './AuthLayout.css'

const AuthLayout = () => {
  const { pathname } = useLocation()
  const isWide = pathname === '/signup'

  return (
    <div className="auth-layout">
      <div className={isWide ? 'auth-container auth-container--wide' : 'auth-container'}>
        <Outlet />
      </div>
    </div>
  )
}

export default AuthLayout