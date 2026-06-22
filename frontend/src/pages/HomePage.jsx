import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { myFinesSummary } from '../api/lendingApi.js'
import { formatMoney } from '../utils/format.js'

export default function HomePage() {
  const { user, role } = useAuth()
  const [summary, setSummary] = useState(null)

  useEffect(() => {
    if (role === 'STUDENT') myFinesSummary().then(setSummary).catch(() => {})
  }, [role])

  const isStaff = role === 'LIBRARIAN' || role === 'ADMIN'

  return (
    <div className="stack">
      <div className="card">
        <h1>Welcome, {user?.fullName} 👋</h1>
        <p className="muted">Signed in as <span className="badge">{role}</span></p>
      </div>

      {role === 'STUDENT' && (
        <div className="card">
          <h2>Your borrowing status</h2>
          {summary ? (
            <p>
              Outstanding fines: <strong>{formatMoney(summary.totalOutstandingFines)}</strong>{' · '}
              {summary.canBorrow
                ? <span className="badge">Eligible to borrow</span>
                : <span style={{ color: 'var(--danger)', fontWeight: 600 }}>Borrowing blocked</span>}
            </p>
          ) : <p className="muted">Loading…</p>}
          <div className="actions">
            <Link className="btn-primary inline" to="/books">Browse catalog</Link>
            <Link className="btn-ghost" to="/reservations">My reservations</Link>
            <Link className="btn-ghost" to="/loans">My loans</Link>
            <Link className="btn-ghost" to="/fines">My fines</Link>
          </div>
        </div>
      )}

      {isStaff && (
        <div className="card">
          <h2>Quick actions</h2>
          <div className="actions">
            <Link className="btn-primary inline" to="/loans">Checkout / returns</Link>
            <Link className="btn-ghost" to="/books">Manage catalog</Link>
            <Link className="btn-ghost" to="/categories">Categories</Link>
            <Link className="btn-ghost" to="/fines">Pending fines</Link>
            <Link className="btn-ghost" to="/reservations">Reservations</Link>
          </div>
        </div>
      )}
    </div>
  )
}
