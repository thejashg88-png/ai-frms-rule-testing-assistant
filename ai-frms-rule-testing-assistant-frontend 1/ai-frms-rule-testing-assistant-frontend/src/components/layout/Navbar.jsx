import React from 'react'
import { LogOut, User } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { useNavigate } from 'react-router-dom'
import './layout.css'

const ROLE_STYLE = {
  ADMIN:  { background: '#dcfce7', color: '#16a34a' },
  TESTER: { background: '#eff6ff', color: '#2563eb' },
  VIEWER: { background: '#f1f5f9', color: '#475569' },
}

const Navbar = () => {
  const { user, role, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const displayName = user?.username || user?.fullName || user?.name || user?.email || 'User'
  const roleStyle   = ROLE_STYLE[role] ?? { background: '#f1f5f9', color: '#475569' }

  return (
    <header className="navbar">
      <div className="navbar-left">
        <h2 className="navbar-title">AI FRMS Rule Testing Assistant</h2>
      </div>

      <div className="navbar-right">
        <div className="user-menu">
          <div className="user-info">
            <User size={16} className="user-icon" />
            <span className="user-email">{displayName}</span>
            {role && (
              <span style={{
                ...roleStyle,
                fontSize: 11,
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: 999,
                letterSpacing: '0.4px',
                textTransform: 'uppercase',
                marginLeft: 4,
              }}>
                {role}
              </span>
            )}
          </div>
          <button className="logout-btn" onClick={handleLogout}>
            <LogOut size={14} />
            Logout
          </button>
        </div>
      </div>
    </header>
  )
}

export default Navbar
