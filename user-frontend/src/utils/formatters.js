export function formatCurrency(amount) {
  if (amount == null) return '—'
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatAmount(amount) {
  if (amount == null) return '—'
  return new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatQuantity(qty) {
  if (qty == null) return '—'
  return new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  }).format(qty)
}

export function formatCompact(num) {
  if (num == null) return '—'
  if (num >= 10000000) return `${(num / 10000000).toFixed(2)} Cr`
  if (num >= 100000) return `${(num / 100000).toFixed(2)} L`
  if (num >= 1000) return `${(num / 1000).toFixed(1)} K`
  return num.toString()
}

export function truncateId(id) {
  if (!id) return '—'
  return id.length > 10 ? `${id.slice(0, 4)}...${id.slice(-4)}` : id
}
