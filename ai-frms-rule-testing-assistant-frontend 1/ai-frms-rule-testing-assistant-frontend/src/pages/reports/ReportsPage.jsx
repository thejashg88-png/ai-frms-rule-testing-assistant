import React, { useState, useEffect } from 'react'
import PageHeader from '../../components/common/PageHeader'
import Card from '../../components/common/Card'
import Loader from '../../components/common/Loader'
import ErrorMessage from '../../components/common/ErrorMessage'
import ReportFilter from '../../components/reports/ReportFilter'
import ExecutionReportView from '../../components/reports/ExecutionReportView'
import DownloadReportButtons from '../../components/reports/DownloadReportButtons'
import reportService from '../../services/reportService'

const ReportsPage = () => {
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filters, setFilters] = useState({ days: '7' })

  const load = async () => {
    setLoading(true); setError(null)
    try { setReport(await reportService.getExecutionReport(filters)) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [filters.days])

  return (
    <div>
      <PageHeader
        title="Reports"
        subtitle="Execution statistics and rule performance analytics"
        actions={<DownloadReportButtons />}
      />

      <ReportFilter
        filters={filters}
        onChange={(e) => setFilters((p) => ({ ...p, [e.target.name]: e.target.value }))}
        onRefresh={load}
        loading={loading}
      />

      {loading && <Loader message="Loading report data…" />}
      {error && <ErrorMessage title="Failed to load report" message={error} onRetry={load} />}
      {!loading && !error && report && <ExecutionReportView report={report} />}
    </div>
  )
}

export default ReportsPage
