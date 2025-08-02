import { createBrowserRouter } from 'react-router-dom'
import App from './App'
import { SignupPage } from './pages/auth/SignupPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
  },
  {
    path: '/signup',
    element: <SignupPage />,
  },
], {
  basename: '/web-app'
})