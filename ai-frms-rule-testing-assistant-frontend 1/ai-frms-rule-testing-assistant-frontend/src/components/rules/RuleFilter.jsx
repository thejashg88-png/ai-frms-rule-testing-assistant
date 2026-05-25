import React from 'react'
import Input from '../common/Input'
import Select from '../common/Select'

const STATUS_OPTIONS = [
  { value: 'ACTIVE',   label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const TYPE_OPTIONS = [
  { value: 'CREDIT',      label: 'Credit' },
  { value: 'AMOUNT',      label: 'Amount' },
  { value: 'VELOCITY',    label: 'Velocity' },
  { value: 'GEO',         label: 'Geographic' },
  { value: 'FRAUD',       label: 'Fraud' },
  { value: 'CARD',        label: 'Card' },
  { value: 'TRANSACTION', label: 'Transaction' },
  { value: 'FREQUENCY',   label: 'Frequency' },
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
          options={TYPE_OPTIONS}
          value={filters.ruleType}
          onChange={onChange}
        />
      </div>
    </div>
  )
}

export default RuleFilter
