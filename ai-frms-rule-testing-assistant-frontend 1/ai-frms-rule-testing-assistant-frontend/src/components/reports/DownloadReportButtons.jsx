import React, { useState } from 'react'
import Button from '../common/Button'
import reportService from '../../services/reportService'
import { useToast } from '../../hooks/useToast'
import { useAuth } from '../../hooks/useAuth'

const DownloadReportButtons = () => {
  const { addToast } = useToast()
  const { isAdmin, isTester } = useAuth()
  const [downloading, setDownloading] = useState(null)

  if (!isAdmin && !isTester) {
    return (
      <span style={{
        fontSize: 12,
        color: 'var(--text-secondary)',
        padding: '6px 12px',
        border: '1px solid var(--border)',
        borderRadius: 6,
      }}>
        Download requires ADMIN or TESTER role
      </span>
    )
  }

  const download = async (type) => {
    setDownloading(type)
    try {
      await reportService.downloadReport(type)
      addToast(`${type} report downloaded`, 'success')
    } catch (err) {
      addToast(err.message, 'error')
    } finally {
      setDownloading(null)
    }
  }

  return (
    <div style={{ display: 'flex', gap: 10 }}>
      <Button variant="outline" size="sm" loading={downloading === 'executions'} onClick={() => download('executions')}>
        Download Execution Report
      </Button>
      <Button variant="outline" size="sm" loading={downloading === 'rules'} onClick={() => download('rules')}>
        Download Rule Report
      </Button>
    </div>
  )
}

export default DownloadReportButtons
