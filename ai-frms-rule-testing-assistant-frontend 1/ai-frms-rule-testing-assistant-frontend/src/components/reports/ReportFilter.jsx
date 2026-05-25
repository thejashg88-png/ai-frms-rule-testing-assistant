import React from 'react'
import Select from '../common/Select'
import Button from '../common/Button'

const DATE_RANGE_OPTIONS = [
  { value: '7',  label: 'Last 7 days' },
  { value: '30', label: 'Last 30 days' },
  { value: '90', label: 'Last 90 days' },
]

const ReportFilter = ({ filters, onChange, onRefresh, loading }) => (
  <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 24 }}>
    <div style={{ width: 180 }}>
      <Select label="Date Range" name="days" options={DATE_RANGE_OPTIONS}
        value={filters.days ?? '7'} onChange={onChange} />
    </div>
    <div style={{ paddingBottom: 16 }}>
      <Button variant="outline" onClick={onRefresh} loading={loading}>Refresh</Button>
    </div>
  </div>
)

export default ReportFilter
