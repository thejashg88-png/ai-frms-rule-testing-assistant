export const dateUtils = {
  // Format date to DD/MM/YYYY
  formatDate: (date) => {
    if (!date) return ''
    const d = new Date(date)
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${day}/${month}/${d.getFullYear()}`
  },

  // Format datetime to DD/MM/YYYY HH:mm:ss
  formatDateTime: (date) => {
    if (!date) return ''
    const d = new Date(date)
    const dateStr = this.formatDate(date)
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    const seconds = String(d.getSeconds()).padStart(2, '0')
    return `${dateStr} ${hours}:${minutes}:${seconds}`
  },

  // Format time to HH:mm:ss
  formatTime: (date) => {
    if (!date) return ''
    const d = new Date(date)
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    const seconds = String(d.getSeconds()).padStart(2, '0')
    return `${hours}:${minutes}:${seconds}`
  },

  // Get relative time (e.g., "2 hours ago")
  getRelativeTime: (date) => {
    const d = new Date(date)
    const now = new Date()
    const diffMs = now - d
    const diffSecs = Math.round(diffMs / 1000)
    const diffMins = Math.round(diffSecs / 60)
    const diffHours = Math.round(diffMins / 60)
    const diffDays = Math.round(diffHours / 24)

    if (diffSecs < 60) return 'just now'
    if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
    if (diffDays < 30) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
    
    return this.formatDate(date)
  },

  // Check if date is today
  isToday: (date) => {
    const d = new Date(date)
    const today = new Date()
    return d.toDateString() === today.toDateString()
  },

  // Add days to date
  addDays: (date, days) => {
    const d = new Date(date)
    d.setDate(d.getDate() + days)
    return d
  },

  // Get start of day
  startOfDay: (date) => {
    const d = new Date(date)
    d.setHours(0, 0, 0, 0)
    return d
  },

  // Get end of day
  endOfDay: (date) => {
    const d = new Date(date)
    d.setHours(23, 59, 59, 999)
    return d
  },
}

export default dateUtils