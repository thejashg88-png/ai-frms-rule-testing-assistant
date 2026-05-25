import React, { useState } from 'react'
import Button from '../common/Button'
import reportService from '../../services/reportService'
import { useToast } from '../../hooks/useToast'

const DownloadReportButtons = () => {
  const { addToast } = useToast()
  const [downloading, setDownloading] = useState(null)

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
