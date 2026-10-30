import { useEffect, useState } from 'react'
import { getDepartments, getRoles } from '../api/lookups'

// Departments and roles rarely change: fetch once and share between pages.
let cached = null

/** { departments, roles, loading, error } */
export function useLookups() {
  const [state, setState] = useState({ departments: [], roles: [], loading: true, error: false })

  useEffect(() => {
    let active = true
    cached ??= Promise.all([getDepartments(), getRoles()]).catch((error) => {
      cached = null // allow a retry next time
      throw error
    })
    cached
      .then(([departments, roles]) => active && setState({ departments, roles, loading: false, error: false }))
      .catch(() => active && setState((s) => ({ ...s, loading: false, error: true })))
    return () => {
      active = false
    }
  }, [])

  return state
}
