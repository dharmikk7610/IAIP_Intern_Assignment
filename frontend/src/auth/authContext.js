import { createContext } from 'react'

export const AuthContext = createContext(null)

export const MANAGER_ROLES = ['ADMIN', 'MANAGER']

/** Where a user lands after login: employees list for ADMIN/MANAGER, own profile for EMPLOYEE. */
export function homePath(user) {
  if (user?.role === 'EMPLOYEE') {
    return user.employeeId ? `/employees/${user.employeeId}` : '/login'
  }
  return '/employees'
}
