import { useContext } from 'react'
import { AuthContext } from './authContext'

/** { user, login, logout, hasRole } */
export function useAuth() {
  return useContext(AuthContext)
}
