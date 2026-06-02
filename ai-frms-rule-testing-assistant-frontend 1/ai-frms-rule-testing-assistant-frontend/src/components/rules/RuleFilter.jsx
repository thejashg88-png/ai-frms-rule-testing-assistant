import React from 'react'
import Input from '../common/Input'
import Select from '../common/Select'
import { RULE_TYPES } from '../../data/ruleTypes'

const STATUS_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const RuleFilter = ({ filters, onChange }) => {
  return (
    <div className="rules-filters">
      <div className="rules-search-wrapper">
        <Input
          name="search"
          placeholder="Search by name or description…"
          value={filters.search}
          onChange={onChange}
        />
      </div>
      <div className="rules-filter-select-wrapper">
        <Select
          name="status"
          placeholder="All Statuses"
          options={STATUS_OPTIONS}
          value={filters.status}
          onChange={onChange}
        />
      </div>
      <div className="rules-filter-select-wrapper">
        <Select
          name="ruleType"
          placeholder="All Types"
          options={RULE_TYPES}
          value={filters.ruleType}
          onChange={onChange}
        />
      </div>
    </div>
  )
}

export default RuleFilter
