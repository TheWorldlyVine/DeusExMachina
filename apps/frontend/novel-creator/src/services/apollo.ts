import { ApolloClient, InMemoryCache, createHttpLink, ApolloLink } from '@apollo/client'
import { setContext } from '@apollo/client/link/context'
import { onError } from '@apollo/client/link/error'
import toast from 'react-hot-toast'

const httpLink = createHttpLink({
  uri: import.meta.env.VITE_GRAPHQL_URL || '/graphql',
})

const authLink = setContext((_, { headers }) => {
  const token = localStorage.getItem('auth_token')
  const projectId = localStorage.getItem('current_project_id')
  
  return {
    headers: {
      ...headers,
      authorization: token ? `Bearer ${token}` : '',
      ...(projectId && { 'X-Project-ID': projectId }),
    },
  }
})

const errorLink = onError(({ graphQLErrors, networkError }) => {
  if (graphQLErrors) {
    graphQLErrors.forEach(({ message, locations, path }) => {
      console.error(
        `[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`
      )
      toast.error(`GraphQL Error: ${message}`)
    })
  }

  if (networkError) {
    console.error(`[Network error]: ${networkError}`)
    toast.error('Network error. Please check your connection.')
  }
})

export const apolloClient = new ApolloClient({
  link: ApolloLink.from([errorLink, authLink, httpLink]),
  cache: new InMemoryCache({
    typePolicies: {
      Document: {
        keyFields: ['id'],
      },
      Chapter: {
        keyFields: ['id'],
      },
      Scene: {
        keyFields: ['id'],
      },
      Memory: {
        keyFields: ['id'],
      },
    },
  }),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network',
    },
  },
})