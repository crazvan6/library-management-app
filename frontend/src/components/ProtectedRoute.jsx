import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

// Guards a route: requires authentication, and optionally one of `roles`.
export default function ProtectedRoute({ children, roles }) {
  const { isAuthenticated, role } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  if (roles && !roles.includes(role)) {
    return <Navigate to="/forbidden" replace />
  }
  return children
}
