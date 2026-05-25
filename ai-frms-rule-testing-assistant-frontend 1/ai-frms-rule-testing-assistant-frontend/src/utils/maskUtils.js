export const maskCardNumber = (cardNumber) => {
  if (!cardNumber) return '****-****-****-****'
  const str = String(cardNumber).replace(/\D/g, '')
  if (str.length <= 4) return str
  return `****-****-****-${str.slice(-4)}`
}

export const maskEmail = (email) => {
  if (!email) return ''
  const [user, domain] = email.split('@')
  if (!user || !domain) return email
  return `${user.slice(0, 2)}****@${domain}`
}
