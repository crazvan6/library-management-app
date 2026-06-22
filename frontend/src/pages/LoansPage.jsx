import { useCallback, useEffect, useState } from 'react'
import { myLoans, activeLoans, overdueLoans, checkout, returnBook } from '../api/lendingApi.js'
import { apiErrorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { formatDate, formatMoney } from '../utils/format.js'

export default function LoansPage() {
  const { role } = useAuth()
  const isStaff = role === 'LIBRARIAN' || role === 'ADMIN'
  return isStaff ? <StaffLoans /> : <StudentLoans />
}

function LoanTable({ loans, onReturn }) {
  return (
    <table className="table">
      <thead>
        <tr><th>Book</th><th>Borrower</th><th>Due</th><th>Status</th><th>Overdue</th>{onReturn && <th></th>}</tr>
      </thead>
      <tbody>
        {loans.length === 0 && <tr><td colSpan={onReturn ? 6 : 5} className="muted">No loans.</td></tr>}
        {loans.map((l) => (
          <tr key={l.loanId}>
            <td>{l.bookTitle}</td>
            <td>{l.userFullName}</td>
            <td>{formatDate(l.dueDate)}</td>
            <td><span className="badge">{l.status}</span></td>
            <td>{l.daysOverdue > 0 ? `${l.daysOverdue}d` : '—'}</td>
            {onReturn && <td>{l.status !== 'RETURNED' && <button className="btn-ghost" onClick={() => onReturn(l.loanId)}>Return</button>}</td>}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function StudentLoans() {
  const [loans, setLoans] = useState([])
  const [error, setError] = useState(null)
  useEffect(() => { myLoans().then(setLoans).catch((e) => setError(apiErrorMessage(e))) }, [])
  return (
    <div className="stack">
      <h1>My loans</h1>
      {error && <div className="alert error">{error}</div>}
      <div className="card"><LoanTable loans={loans} /></div>
    </div>
  )
}

function StaffLoans() {
  const [active, setActive] = useState([])
  const [overdue, setOverdue] = useState([])
  const [form, setForm] = useState({ userId: '', bookId: '', reservationId: '' })
  const [msg, setMsg] = useState(null)
  const [error, setError] = useState(null)

  const reload = useCallback(() => {
    activeLoans().then(setActive).catch(() => {})
    overdueLoans().then(setOverdue).catch(() => {})
  }, [])
  useEffect(() => { reload() }, [reload])

  const onCheckout = async (e) => {
    e.preventDefault(); setError(null); setMsg(null)
    if (!form.userId || !form.bookId) { setError('Student user ID and Book ID are required'); return }
    try {
      const payload = { userId: Number(form.userId), bookId: Number(form.bookId) }
      if (form.reservationId) payload.reservationId = Number(form.reservationId)
      const res = await checkout(payload)
      setMsg(`Checked out "${res.loan?.bookTitle}" — due ${formatDate(res.loan?.dueDate)}`)
      setForm({ userId: '', bookId: '', reservationId: '' })
      reload()
    } catch (err) { setError(apiErrorMessage(err, 'Checkout failed')) }
  }

  const onReturn = async (loanId) => {
    setError(null); setMsg(null)
    try {
      const r = await returnBook(loanId)
      setMsg(`${r.message}${r.fine ? ` — fine ${formatMoney(r.fine.amount)}` : ''}`)
      reload()
    } catch (err) { setError(apiErrorMessage(err, 'Return failed')) }
  }

  return (
    <div className="stack">
      <h1>Loans</h1>
      {msg && <div className="alert success">{msg}</div>}
      {error && <div className="alert error">{error}</div>}

      <form className="card filters" onSubmit={onCheckout}>
        <strong style={{ flexBasis: '100%' }}>Check out a book</strong>
        <input placeholder="Student user ID" value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })} />
        <input placeholder="Book ID" value={form.bookId} onChange={(e) => setForm({ ...form, bookId: e.target.value })} />
        <input placeholder="Reservation ID (optional)" value={form.reservationId} onChange={(e) => setForm({ ...form, reservationId: e.target.value })} />
        <button className="btn-primary inline" type="submit">Checkout</button>
      </form>

      <div className="card">
        <h2>Active loans</h2>
        <LoanTable loans={active} onReturn={onReturn} />
      </div>
      <div className="card">
        <h2>Overdue loans</h2>
        <LoanTable loans={overdue} onReturn={onReturn} />
      </div>
    </div>
  )
}
