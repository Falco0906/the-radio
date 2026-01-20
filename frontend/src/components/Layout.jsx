import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './Layout.css'

const Layout = ({ children }) => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-title">The Radio</div>
        <nav className="app-nav">
          <Link to="/dashboard">Dashboard</Link>
          <Link to="/profile">Profile</Link>
          {user && (
            <button onClick={handleLogout} className="logout-btn">
              Logout
            </button>
          )}
        </nav>
      </header>
      <main className="app-main">{children}</main>
    </div>
  )
}

export default Layout

