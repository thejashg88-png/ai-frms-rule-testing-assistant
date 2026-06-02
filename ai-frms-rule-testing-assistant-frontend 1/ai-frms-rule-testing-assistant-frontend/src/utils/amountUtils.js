/**
 * Formats a numeric amount as a localized currency string.
 * Falls back to 'INR' when currency is missing or invalid — prevents Intl.NumberFormat
 * from throwing on an unknown currency code returned by the backend.
 * Returns '-' for null/undefined/empty to avoid blank cells in tables.
 */
export const formatAmount = (amount, currency = 'INR') => {
  if (amount === null || amount === undefined || amount === '') return '-'

  const safeCurrency =
    typeof currency === 'string' && currency.trim()
      ? currency.trim().toUpperCase()
      : 'INR'

  const numericAmount = Number(amount)
  if (Number.isNaN(numericAmount)) return String(amount)

  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: safeCurrency,
      minimumFractionDigits: 2,
    }).format(numericAmount)
  } catch (error) {
    console.warn('[formatAmount] Invalid currency, falling back:', safeCurrency)
    return `${numericAmount}`
  }
}

export const formatINR = (amount) => formatAmount(amount, 'INR')

export const parseAmount = (str) => {
  const n = parseFloat(String(str ?? '').replace(/[^0-9.]/g, ''))
  return isNaN(n) ? null : n
}
