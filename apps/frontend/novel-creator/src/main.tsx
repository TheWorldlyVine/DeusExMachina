import React from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { ApolloProvider } from '@apollo/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { Toaster } from 'react-hot-toast'

import App from './App'
import { store } from './store'
import { apolloClient } from './services/apollo'
import './styles/index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 1,
    },
  },
})

// Handle SPA routing redirect
const handleSPARedirect = () => {
  const redirectPath = sessionStorage.getItem('spa-redirect-path');
  if (redirectPath && redirectPath.startsWith('/novel-creator/')) {
    sessionStorage.removeItem('spa-redirect-path');
    // Remove the base path to get the route
    const route = redirectPath.replace('/novel-creator', '');
    if (route && route !== '/') {
      window.history.replaceState(null, '', redirectPath);
    }
  }
};

handleSPARedirect();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <ApolloProvider client={apolloClient}>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter basename="/novel-creator">
            <App />
            <Toaster position="bottom-right" />
          </BrowserRouter>
          <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
      </ApolloProvider>
    </Provider>
  </React.StrictMode>
)