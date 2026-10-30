import { Navigate, Route, Routes } from 'react-router-dom'
import { MANAGER_ROLES, homePath } from './auth/authContext'
import RequireAuth from './auth/RequireAuth'
import { useAuth } from './auth/useAuth'
import Layout from './components/Layout'
import EmployeeDetailsPage from './pages/EmployeeDetailsPage'
import EmployeeFormPage from './pages/EmployeeFormPage'
import EmployeeListPage from './pages/EmployeeListPage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import TransferPage from './pages/TransferPage'

function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={homePath(user)} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<HomeRedirect />} />
        <Route
          path="employees"
          element={
            <RequireAuth roles={MANAGER_ROLES}>
              <EmployeeListPage />
            </RequireAuth>
          }
        />
        <Route
          path="employees/new"
          element={
            <RequireAuth roles={MANAGER_ROLES}>
              <EmployeeFormPage key="new" />
            </RequireAuth>
          }
        />
        {/* EMPLOYEE users may open their own details page; the API rejects anyone else's */}
        <Route path="employees/:id" element={<EmployeeDetailsPage />} />
        <Route
          path="employees/:id/edit"
          element={
            <RequireAuth roles={MANAGER_ROLES}>
              <EmployeeFormPage key="edit" />
            </RequireAuth>
          }
        />
        <Route
          path="employees/:id/transfer"
          element={
            <RequireAuth roles={MANAGER_ROLES}>
              <TransferPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
