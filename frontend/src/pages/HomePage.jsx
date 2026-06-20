import { useAuth } from '../auth/AuthContext.jsx'

const ROLE_BLURB = {
  STUDENT: 'Browse the catalog, reserve books, and track your loans and fines.',
  LIBRARIAN: 'Manage the catalog, check out and return books, and process fines.',
  ADMIN: 'Full administration: users, catalog, loans, and fine waivers.',
}

export default function HomePage() {
  const { user, role } = useAuth()

  return (
    <div className="stack">
      <div className="card">
        <h1>Welcome, {user?.fullName} 👋</h1>
        <p className="muted">
          You are signed in as <span className="badge">{role}</span>
        </p>
        <p>{ROLE_BLURB[role] || 'Welcome to the Library Management System.'}</p>
      </div>

      <div className="card">
        <h2>What's here</h2>
        <ul>
          <li><strong>Catalog</strong> — search and manage books &amp; categories <span className="muted">(coming in FE-2)</span></li>
          <li><strong>Reservations &amp; loans</strong> — reserve, check out, return <span className="muted">(coming in FE-3)</span></li>
          <li><strong>Fines</strong> — view, pay, and waive fines <span className="muted">(coming in FE-3)</span></li>
        </ul>
      </div>
    </div>
  )
}
