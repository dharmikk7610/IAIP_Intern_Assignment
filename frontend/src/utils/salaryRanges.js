/**
 * Salary filter presets. min is inclusive, max is exclusive (same rule as the API),
 * so "₹3–5 LPA" means 3,00,000 <= salary < 5,00,000 and the ranges never overlap.
 */
export const SALARY_RANGES = [
  { value: '1-3', label: '₹1–3 LPA', min: 100000, max: 300000 },
  { value: '3-5', label: '₹3–5 LPA', min: 300000, max: 500000 },
  { value: '5-10', label: '₹5–10 LPA', min: 500000, max: 1000000 },
  { value: '10+', label: '₹10+ LPA', min: 1000000, max: null },
  { value: 'custom', label: 'Custom range' },
]

/** Turns the selected preset (or custom values) into { minSalary, maxSalary } query parameters. */
export function salaryParams(rangeValue, customMin, customMax) {
  if (rangeValue === 'custom') {
    return { minSalary: customMin || undefined, maxSalary: customMax || undefined }
  }
  const range = SALARY_RANGES.find((r) => r.value === rangeValue)
  return range ? { minSalary: range.min, maxSalary: range.max ?? undefined } : {}
}
