import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { store } from './store'
import App from './App'

describe('App', () => {
  it('renders login page when not authenticated', async () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </Provider>
    )
    
    // Since there's no auth token, it should show the login page
    expect(await screen.findByText(/Sign in to Novel Creator/i)).toBeInTheDocument()
  })
})