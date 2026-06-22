export function formatDate(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleDateString()
}

export function formatMoney(amount) {
  if (amount == null) return '€0.00'
  return `€${Number(amount).toFixed(2)}`
}
