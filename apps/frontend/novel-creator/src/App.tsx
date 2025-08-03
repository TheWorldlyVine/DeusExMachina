import { Routes, Route, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { checkAuth } from '@/features/auth/authSlice'
import { Layout } from '@/components/layout/Layout'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { LoadingScreen } from '@/components/common/LoadingScreen'

// Import debug utils in development
if (import.meta.env.DEV) {
  import('@/utils/debug')
}

// Pages
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { EditorPage } from '@/features/editor/pages/EditorPage'
import { DocumentsPage } from '@/features/documents/pages/DocumentsPage'
import { SettingsPage } from '@/features/settings/pages/SettingsPage'

function App() {
  const dispatch = useAppDispatch()
  const { isLoading, isAuthenticated } = useAppSelector((state) => state.auth)

  useEffect(() => {
    dispatch(checkAuth())
  }, [dispatch])

  if (isLoading) {
    return <LoadingScreen />
  }

  return (
    <Routes>
      <Route path="/login" element={!isAuthenticated ? <LoginPage /> : <Navigate to="/" />} />
      
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/editor/:documentId" element={<EditorPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>
      
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App