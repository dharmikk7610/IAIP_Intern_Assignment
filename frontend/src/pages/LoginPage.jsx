import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { homePath } from '../auth/authContext'
import { useAuth } from '../auth/useAuth'

const DEMO_USERS = [
  { username: 'admin', password: 'Admin@123', role: 'Admin — full access' },
  { username: 'manager', password: 'Manager@123', role: 'Manager — no deactivate, masked Aadhaar/PAN' },
  { username: 'rahul', password: 'Employee@123', role: 'Employee — own profile only' },
]

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to={homePath(user)} replace />
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!form.username.trim() || !form.password) {
      setError('Enter your username and password')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const loggedIn = await login(form.username.trim(), form.password)
      const from = location.state?.from?.pathname
      navigate(from && loggedIn.role !== 'EMPLOYEE' ? from : homePath(loggedIn), { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, 'Login failed'))
      setSubmitting(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <span className="brand-mark brand-mark-lg" aria-hidden="true">E</span>
          <h1>Employee Management</h1>
          <p className="muted">Sign in to continue</p>
        </div>
        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              autoComplete="username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              autoFocus
            />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>
          {error && (
            <p className="alert alert-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
       
      </div>
    </div>
  )
}
