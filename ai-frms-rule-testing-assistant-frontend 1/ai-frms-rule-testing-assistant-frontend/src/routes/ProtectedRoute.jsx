import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import Loader from '../components/common/Loader'
import AccessDeniedPage from '../pages/error/AccessDeniedPage'

const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) return <Loader fullScreen message="Checking session..." />
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Outlet />
}

// Wraps a single page element and shows AccessDeniedPage if the user's role
// is not in the allowed list. Used on individual routes in AppRoutes.
export const RoleGuard = ({ roles, children }) => {
  const { role, isLoading } = useAuth()
  if (isLoading) return <Loader message="Checking permissions..." />
  if (!roles.includes(role)) return <AccessDeniedPage />
  return children
}

export default ProtectedRoute
