import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { apiErrorMessage } from '../api/client.js'
import { validateEmail, validateRequired } from '../utils/validation.js'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState(null)
  const [loading, setLoading] = useState(false)

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const validate = () => {
    const errs = {
      email: validateEmail(form.email),
      password: validateRequired(form.password, 'Password'),
    }
    setErrors(errs)
    return !errs.email && !errs.password
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setServerError(null)
    if (!validate()) return
    setLoading(true)
    try {
      await login(form.email.trim(), form.password)
      navigate(location.state?.from?.pathname || '/', { replace: true })
    } catch (err) {
      setServerError(apiErrorMessage(err, 'Invalid email or password'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-card">
      <h1>Sign in</h1>
      {serverError && <div className="alert error">{serverError}</div>}
      <form onSubmit={onSubmit} noValidate>
        <label>
          Email
          <input name="email" type="email" value={form.email} onChange={onChange} autoComplete="username" />
          {errors.email && <span className="field-error">{errors.email}</span>}
        </label>
        <label>
          Password
          <input name="password" type="password" value={form.password} onChange={onChange} autoComplete="current-password" />
          {errors.password && <span className="field-error">{errors.password}</span>}
        </label>
        <button className="btn-primary" disabled={loading}>{loading ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <p className="muted">No account? <Link to="/register">Register as a student</Link></p>
    </div>
  )
}
