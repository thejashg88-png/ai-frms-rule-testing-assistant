import React, { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { MENU_ITEMS, MENU_SECTIONS } from '../../data/menuItems'
import './layout.css'

const Sidebar = () => {
  const location = useLocation()
  const [isExpanded, setIsExpanded] = useState(true)

  const isActive = (path) => location.pathname === path

  return (
    <aside className={`sidebar ${isExpanded ? 'expanded' : 'collapsed'}`}>
      <div className="sidebar-header">
        {isExpanded && <h1 className="sidebar-logo">AI FRMS</h1>}
        <button
          className="sidebar-toggle"
          onClick={() => setIsExpanded(!isExpanded)}
          title={isExpanded ? 'Collapse sidebar' : 'Expand sidebar'}
        >
          {isExpanded ? <ChevronLeft size={18} /> : <ChevronRight size={18} />}
        </button>
      </div>

      <nav className="sidebar-nav">
        {Object.entries(MENU_SECTIONS).map(([key, label]) => {
          const items = MENU_ITEMS.filter((item) => item.section === key)
          if (items.length === 0) return null

          return (
            <div key={key} className="nav-section">
              {isExpanded && <h3 className="nav-section-title">{label}</h3>}
              {items.map((item) => {
                const Icon = item.icon
                return (
                  <Link
                    key={item.id}
                    to={item.path}
                    className={`nav-item ${isActive(item.path) ? 'active' : ''}`}
                    title={!isExpanded ? item.label : undefined}
                  >
                    <span className="nav-icon"><Icon size={18} /></span>
                    {isExpanded && <span className="nav-label">{item.label}</span>}
                  </Link>
                )
              })}
            </div>
          )
        })}
      </nav>

      <div className="sidebar-footer">
        {isExpanded && <p>© 2025 FRMS</p>}
      </div>
    </aside>
  )
}

export default Sidebar
