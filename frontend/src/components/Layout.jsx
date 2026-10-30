import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { MANAGER_ROLES } from '../auth/authContext'
import { useAuth } from '../auth/useAuth'

export default function Layout() {
  const { user, hasRole, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <span className="brand">
            <span className="brand-mark" aria-hidden="true">E</span>
            Employee Management
          </span>
          <nav className="nav">
            {hasRole(...MANAGER_ROLES) ? (
              <>
                <NavLink to="/employees" end>
                  Employees
                </NavLink>
                <NavLink to="/employees/new">Add Employee</NavLink>
              </>
            ) : (
              <NavLink to={`/employees/${user.employeeId}`}>My Profile</NavLink>
            )}
          </nav>
          <div className="user-box">
            <span className="user-name">
              {user.username} <span className="role-tag">{user.role}</span>
            </span>
            <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </div>
  )
}
