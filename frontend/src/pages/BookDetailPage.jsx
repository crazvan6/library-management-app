import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getBook, deleteBook } from '../api/catalogApi.js'
import { createReservation } from '../api/lendingApi.js'
import { apiErrorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'

export default function BookDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { role } = useAuth()
  const isStaff = role === 'LIBRARIAN' || role === 'ADMIN'
  const isAdmin = role === 'ADMIN'
  const isStudent = role === 'STUDENT'

  const [book, setBook] = useState(null)
  const [error, setError] = useState(null)
  const [msg, setMsg] = useState(null)

  useEffect(() => {
    getBook(id).then(setBook).catch((e) => setError(apiErrorMessage(e, 'Book not found')))
  }, [id])

  const onDelete = async () => {
    if (!window.confirm('Delete this book? This cannot be undone.')) return
    try {
      await deleteBook(id)
      navigate('/books')
    } catch (err) {
      setError(apiErrorMessage(err, 'Delete failed'))
    }
  }

  const onReserve = async () => {
    setError(null); setMsg(null)
    try {
      await createReservation(Number(id))
      setMsg('Book reserved! Track it under "My reservations".')
    } catch (err) {
      setError(apiErrorMessage(err, 'Reservation failed'))
    }
  }

  if (error && !book) return <div className="alert error">{error}</div>
  if (!book) return <p className="muted">Loading…</p>

  return (
    <div className="stack">
      <div className="toolbar">
        <h1>{book.title}</h1>
        <div className="actions">
          <Link className="btn-ghost" to="/books">← Back</Link>
          {isStudent && <button className="btn-primary inline" onClick={onReserve}>Reserve</button>}
          {isStaff && <Link className="btn-primary inline" to={`/books/${book.bookId}/edit`}>Edit</Link>}
          {isAdmin && <button className="btn-danger" onClick={onDelete}>Delete</button>}
        </div>
      </div>

      {msg && <div className="alert success">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      <div className="card book-detail">
        <dl>
          <dt>Author</dt><dd>{book.author}</dd>
          <dt>ISBN</dt><dd>{book.isbn}</dd>
          <dt>Published</dt><dd>{book.publicationYear || '—'}</dd>
          <dt>Status</dt><dd><span className="badge">{book.status}</span></dd>
          <dt>Availability</dt><dd>{book.availableQuantity} of {book.quantity} available</dd>
          <dt>Categories</dt><dd>{(book.categories || []).map((c) => c.name).join(', ') || '—'}</dd>
        </dl>
      </div>
    </div>
  )
}
