export const RULE_TYPES = [
  { value: 'CREDIT',      label: 'Credit' },
  { value: 'AMOUNT',      label: 'Amount' },
  { value: 'VELOCITY',    label: 'Velocity' },
  { value: 'GEO',         label: 'Geographic' },
  { value: 'FRAUD',       label: 'Fraud Detection' },
  { value: 'CARD',        label: 'Card' },
  { value: 'TRANSACTION', label: 'Transaction' },
  { value: 'FREQUENCY',   label: 'Frequency' },
]

export const RULE_TYPE_MAP = Object.fromEntries(RULE_TYPES.map((t) => [t.value, t.label]))

export default RULE_TYPES
