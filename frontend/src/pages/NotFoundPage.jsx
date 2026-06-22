import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="error-page">
      <h1>404</h1>
      <p>The page you're looking for doesn't exist.</p>
      <p><Link to="/">Back to home</Link></p>
    </div>
  )
}
