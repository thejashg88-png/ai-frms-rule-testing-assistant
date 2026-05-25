export const formatAmount = (amount, currency = 'USD') => {
  if (amount == null) return '-'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount)
}

export const formatINR = (amount) => formatAmount(amount, 'INR')

export const parseAmount = (str) => {
  const n = parseFloat(String(str ?? '').replace(/[^0-9.]/g, ''))
  return isNaN(n) ? null : n
}
