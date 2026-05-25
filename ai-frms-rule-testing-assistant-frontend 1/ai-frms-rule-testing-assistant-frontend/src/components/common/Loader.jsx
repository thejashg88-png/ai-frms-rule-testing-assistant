import React from 'react'
import './styles.css'

const Loader = ({ size = 'md', fullScreen = false, message = 'Loading...' }) => {
  const loaderContent = (
    <div className={`loader loader-${size}`}>
      <div className="spinner"></div>
      {message && <p className="loader-message">{message}</p>}
    </div>
  )

  if (fullScreen) {
    return <div className="loader-fullscreen">{loaderContent}</div>
  }

  return loaderContent
}

export default Loader