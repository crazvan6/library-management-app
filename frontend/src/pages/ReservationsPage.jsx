import { useCallback, useEffect, useState } from 'react'
import { myReservations, cancelReservation, reservationsForBook } from '../api/lendingApi.js'
import { apiErrorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { formatDate } from '../utils/format.js'

export default function ReservationsPage() {
  const { role } = useAuth()
  return role === 'STUDENT' ? <StudentReservations /> : <StaffReservations />
}

function StudentReservations() {
  const [items, setItems] = useState([])
  const [error, setError] = useState(null)
  const [msg, setMsg] = useState(null)

  const reload = useCallback(() => {
    myReservations().then(setItems).catch((e) => setError(apiErrorMessage(e)))
  }, [])
  useEffect(() => { reload() }, [reload])

  const onCancel = async (id) => {
    if (!window.confirm('Cancel this reservation?')) return
    setError(null); setMsg(null)
    try { await cancelReservation(id); setMsg('Reservation canceled.'); reload() }
    catch (err) { setError(apiErrorMessage(err, 'Cancel failed')) }
  }

  return (
    <div className="stack">
      <h1>My reservations</h1>
      {msg && <div className="alert success">{msg}</div>}
      {error && <div className="alert error">{error}</div>}
      <div className="card">
        <table className="table">
          <thead><tr><th>Book</th><th>Status</th><th>Expires</th><th>Queue</th><th></th></tr></thead>
          <tbody>
            {items.length === 0 && <tr><td colSpan="5" className="muted">No reservations yet.</td></tr>}
            {items.map((r) => (
              <tr key={r.reservationId}>
                <td>{r.bookTitle}</td>
                <td><span className="badge">{r.status}</span></td>
                <td>{formatDate(r.expiryDate)}</td>
                <td>{r.queuePosition ?? '—'}</td>
                <td>{r.canBeCanceled && <button className="btn-ghost" onClick={() => onCancel(r.reservationId)}>Cancel</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function StaffReservations() {
  const [bookId, setBookId] = useState('')
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)

  const onLookup = async (e) => {
    e.preventDefault(); setError(null)
    if (!bookId) return
    try { setItems(await reservationsForBook(bookId)) }
    catch (err) { setError(apiErrorMessage(err, 'Lookup failed')); setItems(null) }
  }

  return (
    <div className="stack">
      <h1>Reservations by book</h1>
      <form className="card filters" onSubmit={onLookup}>
        <input placeholder="Book ID" value={bookId} onChange={(e) => setBookId(e.target.value)} />
        <button className="btn-primary inline" type="submit">Look up</button>
      </form>
      {error && <div className="alert error">{error}</div>}
      {items && (
        <div className="card">
          <table className="table">
            <thead><tr><th>Reservation</th><th>Student</th><th>Status</th><th>Expires</th><th>Queue</th></tr></thead>
            <tbody>
              {items.length === 0 && <tr><td colSpan="5" className="muted">No reservations for this book.</td></tr>}
              {items.map((r) => (
                <tr key={r.reservationId}>
                  <td>#{r.reservationId}</td>
                  <td>{r.userFullName}</td>
                  <td><span className="badge">{r.status}</span></td>
                  <td>{formatDate(r.expiryDate)}</td>
                  <td>{r.queuePosition ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
