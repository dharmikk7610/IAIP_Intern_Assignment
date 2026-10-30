import { Link } from 'react-router-dom'
import { homePath } from '../auth/authContext'
import { useAuth } from '../auth/useAuth'

export default function NotFoundPage() {
  const { user } = useAuth()
  return (
    <section className="card empty-state">
      <h1>Page not found</h1>
      <p className="muted">The page you are looking for does not exist.</p>
      <Link to={homePath(user)} className="btn btn-primary">
        Go to home
      </Link>
    </section>
  )
}
