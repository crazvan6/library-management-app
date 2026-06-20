import { useCallback, useEffect, useState } from 'react'
import { myFinesSummary, myPendingFines, pendingFines, payFine, waiveFine, PAYMENT_METHODS } from '../api/lendingApi.js'
import { apiErrorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { formatMoney } from '../utils/format.js'

export default function FinesPage() {
  const { role } = useAuth()
  const isStaff = role === 'LIBRARIAN' || role === 'ADMIN'
  return isStaff ? <StaffFines isAdmin={role === 'ADMIN'} /> : <StudentFines />
}

function FineTable({ fines, onPay, onWaive }) {
  const showActions = onPay || onWaive
  return (
    <table className="table">
      <thead>
        <tr><th>Book</th><th>User</th><th>Amount</th><th>Days</th><th>Status</th>{showActions && <th></th>}</tr>
      </thead>
      <tbody>
        {fines.length === 0 && <tr><td colSpan={showActions ? 6 : 5} className="muted">No fines.</td></tr>}
        {fines.map((f) => (
          <tr key={f.fineId}>
            <td>{f.bookTitle}</td>
            <td>{f.userFullName}</td>
            <td>{formatMoney(f.amount)}</td>
            <td>{f.daysOverdue}</td>
            <td><span className="badge">{f.status}</span></td>
            {showActions && (
              <td className="actions">
                {onPay && f.status === 'PENDING' && <button className="btn-ghost" onClick={() => onPay(f.fineId)}>Pay</button>}
                {onWaive && f.status === 'PENDING' && <button className="btn-danger" onClick={() => onWaive(f.fineId)}>Waive</button>}
              </td>
            )}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function StudentFines() {
  const [summary, setSummary] = useState(null)
  const [fines, setFines] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    myFinesSummary().then(setSummary).catch((e) => setError(apiErrorMessage(e)))
    myPendingFines().then(setFines).catch(() => {})
  }, [])

  return (
    <div className="stack">
      <h1>My fines</h1>
      {error && <div className="alert error">{error}</div>}
      {summary && (
        <div className={`alert ${summary.canBorrow ? 'success' : 'error'}`}>
          {summary.canBorrow ? 'You are eligible to borrow.' : 'Borrowing is blocked — outstanding fines too high.'}
          {' · '}Outstanding: <strong>{formatMoney(summary.totalOutstandingFines)}</strong>
        </div>
      )}
      <div className="card"><FineTable fines={fines} /></div>
    </div>
  )
}

function StaffFines({ isAdmin }) {
  const [fines, setFines] = useState([])
  const [method, setMethod] = useState('CASH')
  const [msg, setMsg] = useState(null)
  const [error, setError] = useState(null)

  const reload = useCallback(() => {
    pendingFines().then(setFines).catch((e) => setError(apiErrorMessage(e)))
  }, [])
  useEffect(() => { reload() }, [reload])

  const onPay = async (fineId) => {
    setError(null); setMsg(null)
    try { await payFine(fineId, method); setMsg('Fine marked as paid.'); reload() }
    catch (err) { setError(apiErrorMessage(err, 'Payment failed')) }
  }
  const onWaive = async (fineId) => {
    const reason = window.prompt('Reason for waiving this fine?')
    if (!reason) return
    setError(null); setMsg(null)
    try { await waiveFine(fineId, reason); setMsg('Fine waived.'); reload() }
    catch (err) { setError(apiErrorMessage(err, 'Waive failed')) }
  }

  return (
    <div className="stack">
      <div className="toolbar">
        <h1>Pending fines</h1>
        <label className="checkbox">
          Payment method
          <select value={method} onChange={(e) => setMethod(e.target.value)}>
            {PAYMENT_METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
          </select>
        </label>
      </div>
      {msg && <div className="alert success">{msg}</div>}
      {error && <div className="alert error">{error}</div>}
      <div className="card"><FineTable fines={fines} onPay={onPay} onWaive={isAdmin ? onWaive : null} /></div>
    </div>
  )
}
