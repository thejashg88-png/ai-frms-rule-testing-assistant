import React from 'react'
import { LogOut, User } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { useNavigate } from 'react-router-dom'
import './layout.css'

const Navbar = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="navbar">
      <div className="navbar-left">
        <h2 className="navbar-title">AI FRMS Rule Testing Assistant</h2>
      </div>

      <div className="navbar-right">
        <div className="user-menu">
          <div className="user-info">
            <User size={16} className="user-icon" />
            <span className="user-email">{user?.email || user?.name || 'User'}</span>
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
