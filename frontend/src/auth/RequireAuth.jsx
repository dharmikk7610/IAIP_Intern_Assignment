import { Navigate, useLocation } from 'react-router-dom'
import { homePath } from './authContext'
import { useAuth } from './useAuth'

/**
 * Route guard. Not logged in -> login page (remembering where the user was going).
 * Wrong role -> their home page. The backend enforces the same rules; this only keeps the UI tidy.
 */
export default function RequireAuth({ roles, children }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to={homePath(user)} replace />
  }
  return children
}
