import { Link } from 'react-router-dom'

function App() {
  return (
    <div className="App">
      <h1>DeusExMachina Web App</h1>
      <p>Welcome to the application!</p>
      <div style={{ marginTop: '2rem' }}>
        <Link to="/signup" style={{ color: '#4f46e5', textDecoration: 'underline' }}>
          Go to Signup Page
        </Link>
      </div>
    </div>
  )
}

export default App