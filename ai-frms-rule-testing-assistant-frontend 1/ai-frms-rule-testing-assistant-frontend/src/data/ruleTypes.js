// Backend enum values for RuleType — must match exactly (uppercase, underscore).
// Used in filter dropdowns and table badges. Display labels are in `label`.
// ruleFieldConfig.js maps each value to its required form fields.
export const RULE_TYPES = [
  { value: 'STRUCTURING',        label: 'Structuring' },
  { value: 'UNUSUAL_AMT',        label: 'Unusual Amount' },
  { value: 'HIGH_FREQ_TXN',      label: 'High Frequency Transaction' },
  { value: 'SEQUENTIAL_TXN',     label: 'Sequential Transaction' },
  { value: 'ROUND_AMT_TXN',      label: 'Round Amount Transaction' },
  { value: 'TXN_VELOCITY',       label: 'Transaction Velocity' },
  { value: 'ABNORMAL_HOUR',      label: 'Abnormal Hour' },
  { value: 'INCONSISTENT_MCC',   label: 'Inconsistent MCC' },
  { value: 'SINGLE_LARGE_TX',    label: 'Single Large Transaction' },
  { value: 'MONTHLY_TXN_VOLUME', label: 'Monthly Transaction Volume' },
  { value: 'ANNUAL_TXN_VOLUME',  label: 'Annual Transaction Volume' },
  { value: 'EXCEED_DAILY_LIMIT', label: 'Exceed Daily Limit' },
  { value: 'ANNUAL_TXN_VALUE',   label: 'Annual Transaction Value' },
]

export const RULE_TYPE_MAP = Object.fromEntries(RULE_TYPES.map((t) => [t.value, t.label]))

export const getRuleTypeLabel = (value) => {
  const found = RULE_TYPES.find((type) => type.value === value)
  return found ? found.label : value || 'Unknown'
}

export default RULE_TYPES
