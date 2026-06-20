import { Link } from 'react-router-dom'

export default function ForbiddenPage() {
  return (
    <div className="error-page">
      <h1>403</h1>
      <p>You don't have permission to access this page.</p>
      <p><Link to="/">Back to home</Link></p>
    </div>
  )
}
