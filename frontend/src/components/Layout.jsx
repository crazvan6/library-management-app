import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

export default function Layout() {
  const { isAuthenticated, user, role, logout } = useAuth()

  return (
    <div className="app">
      <header className="navbar">
        <Link to="/" className="brand">📚 Library</Link>
        <nav>
          <NavLink to="/" end>Home</NavLink>
          {/* Catalog / Loans / Fines links are added in the FE-2 and FE-3 pull requests */}
        </nav>
        <div className="nav-right">
          {isAuthenticated ? (
            <>
              <span className="user-chip">{user?.fullName} · {role}</span>
              <button className="btn-link" onClick={logout}>Logout</button>
            </>
          ) : (
            <>
              <NavLink to="/login">Login</NavLink>
              <NavLink to="/register">Register</NavLink>
            </>
          )}
        </div>
      </header>

      <main className="content">
        <Outlet />
      </main>

      <footer className="footer">Library Management System · University project</footer>
    </div>
  )
}
