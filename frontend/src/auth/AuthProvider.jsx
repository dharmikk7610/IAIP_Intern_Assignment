import { useCallback, useEffect, useMemo, useState } from 'react'
import { login as loginRequest } from '../api/auth'
import { setAuthToken, setUnauthorizedHandler } from '../api/client'
import { AuthContext } from './authContext'

const STORAGE_KEY = 'ems.auth'

/**
 * The session ({ token, expiresAt, user }) is kept in localStorage so a page refresh keeps
 * the user logged in. Trade-off: localStorage is readable by any script on the page, so the
 * token is short-lived (60 min) and the app renders no untrusted HTML.
 */
function loadStoredSession() {
  try {
    const session = JSON.parse(localStorage.getItem(STORAGE_KEY))
    if (session?.token && new Date(session.expiresAt) > new Date()) {
      return session
    }
  } catch {
    // corrupt or unavailable storage -> treat as logged out
  }
  return null
}

export default function AuthProvider({ children }) {
  const [session, setSession] = useState(() => {
    const stored = loadStoredSession()
    setAuthToken(stored?.token ?? null)
    return stored
  })

  const logout = useCallback(() => {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignore
    }
    setAuthToken(null)
    setSession(null)
  }, [])

  const login = useCallback(async (username, password) => {
    const data = await loginRequest(username, password)
    const next = { token: data.token, expiresAt: data.expiresAt, user: data.user }
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } catch {
      // still logged in for this tab
    }
    setAuthToken(next.token)
    setSession(next)
    return next.user
  }, [])

  // Any 401 from the API (expired / invalid token) logs the user out
  useEffect(() => setUnauthorizedHandler(logout), [logout])

  // Log out exactly when the token expires
  useEffect(() => {
    if (!session) return undefined
    const timer = setTimeout(logout, new Date(session.expiresAt) - Date.now())
    return () => clearTimeout(timer)
  }, [session, logout])

  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      login,
      logout,
      hasRole: (...roles) => Boolean(session && roles.includes(session.user.role)),
    }),
    [session, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
