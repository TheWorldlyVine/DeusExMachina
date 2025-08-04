import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAppSelector } from '@/hooks/redux'

export function ProtectedRoute() {
  const { isAuthenticated } = useAppSelector((state) => state.auth)
  const location = useLocation()

  if (!isAuthenticated) {
    // Preserve the attempted URL for redirecting after login
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <Outlet />
}