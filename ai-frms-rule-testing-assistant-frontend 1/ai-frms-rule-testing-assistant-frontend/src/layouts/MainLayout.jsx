import React from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from '../components/layout/Sidebar'
import Navbar from '../components/layout/Navbar'
import Footer from '../components/layout/Footer'
import Toast from '../components/common/Toast'
import './MainLayout.css'

const MainLayout = () => {
  return (
    <div className="main-layout">
      <Sidebar />
      <div className="layout-wrapper">
        <Navbar />
        <main className="layout-content">
          <div className="container">
            <Outlet />
          </div>
        </main>
        <Footer />
      </div>
      <Toast />
    </div>
  )
}

export default MainLayout
