import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getErrorMessage, getFieldErrors } from '../api/client'
import { deactivateEmployee, searchEmployees } from '../api/employees'
import { useAuth } from '../auth/useAuth'
import Pagination from '../components/Pagination'
import StatusBadge from '../components/StatusBadge'
import { useToast } from '../components/toastContext'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useLookups } from '../hooks/useLookups'
import { GENDER_OPTIONS, STATUS_OPTIONS, formatCurrency } from '../utils/format'
import { SALARY_RANGES, salaryParams } from '../utils/salaryRanges'

const FILTER_KEYS = ['search', 'departmentId', 'roleId', 'status', 'gender', 'joinedFrom', 'joinedTo',
  'salaryRange', 'minSalary', 'maxSalary']

const COLUMNS = [
  { key: 'employeeCode', label: 'Employee Code' },
  { key: 'fullName', label: 'Name' },
  { key: 'departmentName', label: 'Department' },
  { key: 'roleName', label: 'Role' },
  { key: 'email', label: 'Email' },
  { key: 'mobile', label: 'Mobile' },
  { key: 'annualSalary', label: 'Salary' },
  { key: 'status', label: 'Status' },
]

/**
 * All search / filter / sort / page state lives in the URL query string, so it survives
 * refresh and the back button, and the page can be bookmarked or shared.
 * Every change resets to page 1; the server does the actual filtering.
 */
function toApiQuery(params) {
  const query = {
    pageNumber: params.get('pageNumber') ?? 1,
    pageSize: params.get('pageSize') ?? 10,
    sortBy: params.get('sortBy') ?? undefined,
    sortDir: params.get('sortDir') ?? undefined,
    ...salaryParams(params.get('salaryRange'), params.get('minSalary'), params.get('maxSalary')),
  }
  for (const key of ['search', 'departmentId', 'roleId', 'status', 'gender', 'joinedFrom', 'joinedTo']) {
    if (params.get(key)) query[key] = params.get(key)
  }
  return query
}

export default function EmployeeListPage() {
  const [params, setParams] = useSearchParams()
  const { departments, roles } = useLookups()
  const { hasRole } = useAuth()
  const toast = useToast()

  const [searchText, setSearchText] = useState(params.get('search') ?? '')
  const debouncedSearch = useDebouncedValue(searchText)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  // Which request the shown data belongs to; "loading" is derived from it instead of stored
  const [settledKey, setSettledKey] = useState(null)

  const pageNumber = Number(params.get('pageNumber') ?? 1)
  const pageSize = Number(params.get('pageSize') ?? 10)
  const sortBy = params.get('sortBy')
  const sortDir = params.get('sortDir') ?? 'asc'
  const salaryRange = params.get('salaryRange') ?? ''
  const canManage = hasRole('ADMIN', 'MANAGER')
  const isAdmin = hasRole('ADMIN')

  const updateParams = useCallback(
    (changes, { resetPage = true } = {}) => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          Object.entries(changes).forEach(([key, value]) =>
            value === '' || value == null ? next.delete(key) : next.set(key, value),
          )
          if (resetPage) next.delete('pageNumber')
          return next
        },
        { replace: true },
      )
    },
    [setParams],
  )

  // Push the debounced search text into the URL, only when the text itself changed
  // (not when other params change, which would write back a stale value)
  const lastPushedSearch = useRef(debouncedSearch)
  useEffect(() => {
    if (debouncedSearch === lastPushedSearch.current) return
    lastPushedSearch.current = debouncedSearch
    updateParams({ search: debouncedSearch })
  }, [debouncedSearch, updateParams])

  const apiQuery = useMemo(() => toApiQuery(params), [params])
  const requestKey = `${JSON.stringify(apiQuery)}#${reloadKey}`
  const loading = settledKey !== requestKey

  useEffect(() => {
    let active = true // ignore responses of outdated requests (user typed / clicked again)
    searchEmployees(apiQuery)
      .then((data) => {
        if (!active) return
        // The page no longer exists (e.g. its last row was deactivated): go to the last page that does
        const lastPage = Math.max(data.totalPages, 1)
        if (Number(apiQuery.pageNumber) > lastPage) {
          updateParams({ pageNumber: lastPage === 1 ? '' : lastPage }, { resetPage: false })
          return
        }
        setResult(data)
        setError('')
      })
      .catch((err) => {
        if (!active) return
        // e.g. "Maximum salary must be greater than or equal to minimum salary" instead of "Validation failed"
        const fieldMessages = Object.values(getFieldErrors(err))
        setError(fieldMessages.length > 0 ? fieldMessages.join(' ') : getErrorMessage(err))
        setResult(null) // the old rows don't match the current filters
      })
      .finally(() => active && setSettledKey(requestKey))
    return () => {
      active = false
    }
  }, [apiQuery, requestKey, updateParams])

  const activeFilterCount = FILTER_KEYS.filter((k) => params.get(k) && !['minSalary', 'maxSalary'].includes(k)).length

  const clearFilters = () => {
    setSearchText('')
    lastPushedSearch.current = ''
    updateParams(Object.fromEntries(FILTER_KEYS.map((k) => [k, ''])))
  }

  const toggleSort = (key) => {
    const nextDir = sortBy === key && sortDir === 'asc' ? 'desc' : 'asc'
    updateParams({ sortBy: key, sortDir: nextDir })
  }

  const handleDeactivate = async (employee) => {
    if (!window.confirm(`Deactivate ${employee.fullName} (${employee.employeeCode})? The record is kept.`)) return
    try {
      await deactivateEmployee(employee.id)
      toast.success(`${employee.fullName} has been deactivated`)
      setReloadKey((k) => k + 1)
    } catch (err) {
      toast.error(getErrorMessage(err))
    }
  }

  const commitCustomSalary = (key) => (event) => {
    if (event.type === 'blur' || event.key === 'Enter') {
      updateParams({ [key]: event.target.value })
    }
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Employees</h1>
          <p className="muted">
            {result ? `${result.totalElements} matching record${result.totalElements === 1 ? '' : 's'}` : ' '}
          </p>
        </div>
        {canManage && (
          <Link to="/employees/new" className="btn btn-primary">
            + Add Employee
          </Link>
        )}
      </div>

      <div className="card filters">
        <div className="search-row">
          <input
            type="search"
            className="search-input"
            placeholder="Search by code, name, email, mobile, department, role, address or status…"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            aria-label="Search employees"
          />
          {activeFilterCount > 0 && (
            <button type="button" className="btn btn-ghost" onClick={clearFilters}>
              Clear filters ({activeFilterCount})
            </button>
          )}
        </div>

        <div className="filter-grid">
          <label>
            Department
            <select value={params.get('departmentId') ?? ''} onChange={(e) => updateParams({ departmentId: e.target.value })}>
              <option value="">All departments</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Role / Designation
            <select value={params.get('roleId') ?? ''} onChange={(e) => updateParams({ roleId: e.target.value })}>
              <option value="">All roles</option>
              {roles.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Status
            <select value={params.get('status') ?? ''} onChange={(e) => updateParams({ status: e.target.value })}>
              <option value="">All statuses</option>
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Gender
            <select value={params.get('gender') ?? ''} onChange={(e) => updateParams({ gender: e.target.value })}>
              <option value="">All genders</option>
              {GENDER_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Joined from
            <input type="date" value={params.get('joinedFrom') ?? ''} onChange={(e) => updateParams({ joinedFrom: e.target.value })} />
          </label>
          <label>
            Joined to
            <input type="date" value={params.get('joinedTo') ?? ''} onChange={(e) => updateParams({ joinedTo: e.target.value })} />
          </label>
          <label>
            Annual salary
            <select
              value={salaryRange}
              onChange={(e) => updateParams({ salaryRange: e.target.value, minSalary: '', maxSalary: '' })}
            >
              <option value="">Any salary</option>
              {SALARY_RANGES.map((r) => (
                <option key={r.value} value={r.value}>
                  {r.label}
                </option>
              ))}
            </select>
          </label>
          {salaryRange === 'custom' && (
            <div className="custom-salary">
              <label>
                Min (₹)
                <input
                  type="number"
                  min="0"
                  defaultValue={params.get('minSalary') ?? ''}
                  onBlur={commitCustomSalary('minSalary')}
                  onKeyDown={commitCustomSalary('minSalary')}
                  placeholder="e.g. 250000"
                />
              </label>
              <label>
                Max, below (₹)
                <input
                  type="number"
                  min="0"
                  defaultValue={params.get('maxSalary') ?? ''}
                  onBlur={commitCustomSalary('maxSalary')}
                  onKeyDown={commitCustomSalary('maxSalary')}
                  placeholder="e.g. 800000"
                />
              </label>
            </div>
          )}
        </div>
      </div>

      {error && (
        <p className="alert alert-error" role="alert">
          {error}
        </p>
      )}

      <div className={`card table-card ${loading ? 'is-loading' : ''}`}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                {COLUMNS.map((col) => {
                  const active = sortBy === col.key
                  return (
                    <th key={col.key} aria-sort={active ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'}>
                      <button type="button" className="sort-button" onClick={() => toggleSort(col.key)}>
                        {col.label}
                        <span className={`sort-icon ${active ? 'active' : ''}`} aria-hidden="true">
                          {active ? (sortDir === 'asc' ? '▲' : '▼') : '↕'}
                        </span>
                      </button>
                    </th>
                  )
                })}
                <th className="actions-col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {result?.content.map((e) => (
                <tr key={e.id}>
                  <td>
                    <Link to={`/employees/${e.id}`} className="code-link">
                      {e.employeeCode}
                    </Link>
                  </td>
                  <td className="nowrap">{e.fullName}</td>
                  <td>{e.departmentName}</td>
                  <td>{e.roleName}</td>
                  <td>{e.email}</td>
                  <td>{e.mobile}</td>
                  <td className="num nowrap">{formatCurrency(e.annualSalary)}</td>
                  <td>
                    <StatusBadge status={e.status} />
                  </td>
                  <td className="actions">
                    <Link to={`/employees/${e.id}`} className="btn btn-ghost btn-xs">
                      View
                    </Link>
                    {canManage && (
                      <Link to={`/employees/${e.id}/edit`} className="btn btn-ghost btn-xs">
                        Edit
                      </Link>
                    )}
                    {canManage && e.status !== 'INACTIVE' && (
                      <Link to={`/employees/${e.id}/transfer`} className="btn btn-ghost btn-xs">
                        Transfer
                      </Link>
                    )}
                    {isAdmin && e.status !== 'INACTIVE' && (
                      <button type="button" className="btn btn-danger-ghost btn-xs" onClick={() => handleDeactivate(e)}>
                        Deactivate
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {result && result.content.length === 0 && (
                <tr>
                  <td colSpan={COLUMNS.length + 1} className="empty">
                    No employees match your search and filters.
                  </td>
                </tr>
              )}
              {!result && loading && (
                <tr>
                  <td colSpan={COLUMNS.length + 1} className="empty">
                    Loading employees…
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {result && (
          <Pagination
            pageNumber={pageNumber}
            pageSize={pageSize}
            totalElements={result.totalElements}
            totalPages={result.totalPages}
            onPageChange={(page) => updateParams({ pageNumber: page }, { resetPage: false })}
            onPageSizeChange={(size) => updateParams({ pageSize: size })}
          />
        )}
      </div>
    </section>
  )
}
