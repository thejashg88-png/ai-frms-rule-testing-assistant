import React from 'react'
import Input from '../common/Input'
import Select from '../common/Select'

const STATUS_OPTIONS  = [{ value: 'ACTIVE', label: 'Active' }, { value: 'INACTIVE', label: 'Inactive' }]
const TYPE_OPTIONS    = [{ value: 'SINGLE', label: 'Single Rule' }, { value: 'BULK', label: 'Multi-Rule' }]

const ScenarioFilter = ({ filters, onChange }) => (
  <div className="rules-filters">
    <div className="rules-search-wrapper">
      <Input name="search" placeholder="Search scenarios…" value={filters.search} onChange={onChange} />
    </div>
    <div className="rules-filter-select-wrapper">
      <Select name="status" placeholder="All Statuses" options={STATUS_OPTIONS} value={filters.status} onChange={onChange} />
    </div>
    <div className="rules-filter-select-wrapper">
      <Select name="scenarioType" placeholder="All Types" options={TYPE_OPTIONS} value={filters.scenarioType} onChange={onChange} />
    </div>
  </div>
)

export default ScenarioFilter
