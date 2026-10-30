const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })

/** 450000 -> "₹4,50,000" (Indian digit grouping) */
export function formatCurrency(value) {
  return value == null ? '—' : inr.format(value)
}

/** "2021-06-01" -> "01 Jun 2021". Parsed as a local date to avoid time-zone shifts. */
export function formatDate(isoDate) {
  if (!isoDate) return '—'
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

/** Today as "yyyy-MM-dd" in the user's local time zone (toISOString would use UTC). */
export function todayIso() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** "1995-04-12" + 18 -> "2013-04-12". ISO date strings compare correctly as plain strings. */
export function addYears(isoDate, years) {
  const [year, rest] = [isoDate.slice(0, 4), isoDate.slice(4)]
  return `${Number(year) + years}${rest}`
}

export const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_LEAVE', label: 'On Leave' },
  { value: 'INACTIVE', label: 'Inactive' },
]

export const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
]

export const labelOf = (options, value) => options.find((o) => o.value === value)?.label ?? value ?? '—'
