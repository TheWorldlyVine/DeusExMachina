import { useAppSelector, useAppDispatch } from '@/hooks/redux'
import { logout } from '@/features/auth/authSlice'

export function Header() {
  const dispatch = useAppDispatch()
  const { user } = useAppSelector((state) => state.auth)

  const handleLogout = () => {
    dispatch(logout())
  }

  return (
    <header className="h-16 bg-background border-b border-border px-6 flex items-center justify-between">
      <div className="flex items-center space-x-4">
        <h1 className="text-lg font-semibold">Welcome back, {user?.name || 'Writer'}</h1>
      </div>
      <div className="flex items-center space-x-4">
        <button
          onClick={handleLogout}
          className="text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          Logout
        </button>
      </div>
    </header>
  )
}