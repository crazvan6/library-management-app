import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { apiErrorMessage } from '../api/client.js'
import { validateEmail, validatePassword, validateRequired } from '../utils/validation.js'

const EMPTY = { email: '', password: '', confirmPassword: '', firstName: '', lastName: '', studentId: '' }

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState(null)
  const [loading, setLoading] = useState(false)

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const validate = () => {
    const errs = {
      email: validateEmail(form.email),
      password: validatePassword(form.password),
      confirmPassword: form.confirmPassword === form.password ? null : 'Passwords do not match',
      firstName: validateRequired(form.firstName, 'First name'),
      lastName: validateRequired(form.lastName, 'Last name'),
      studentId: validateRequired(form.studentId, 'Student ID'),
    }
    setErrors(errs)
    return Object.values(errs).every((e) => !e)
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setServerError(null)
    if (!validate()) return
    setLoading(true)
    try {
      await register(form)
      navigate('/', { replace: true })
    } catch (err) {
      setServerError(apiErrorMessage(err, 'Registration failed'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-card">
      <h1>Create account</h1>
      <p className="muted">Public registration creates a <strong>student</strong> account.</p>
      {serverError && <div className="alert error">{serverError}</div>}
      <form onSubmit={onSubmit} noValidate>
        <div className="row2">
          <label>
            First name
            <input name="firstName" value={form.firstName} onChange={onChange} />
            {errors.firstName && <span className="field-error">{errors.firstName}</span>}
          </label>
          <label>
            Last name
            <input name="lastName" value={form.lastName} onChange={onChange} />
            {errors.lastName && <span className="field-error">{errors.lastName}</span>}
          </label>
        </div>
        <label>
          Email
          <input name="email" type="email" value={form.email} onChange={onChange} autoComplete="username" />
          {errors.email && <span className="field-error">{errors.email}</span>}
        </label>
        <label>
          Student ID
          <input name="studentId" value={form.studentId} onChange={onChange} />
          {errors.studentId && <span className="field-error">{errors.studentId}</span>}
        </label>
        <div className="row2">
          <label>
            Password
            <input name="password" type="password" value={form.password} onChange={onChange} autoComplete="new-password" />
            {errors.password && <span className="field-error">{errors.password}</span>}
          </label>
          <label>
            Confirm password
            <input name="confirmPassword" type="password" value={form.confirmPassword} onChange={onChange} autoComplete="new-password" />
            {errors.confirmPassword && <span className="field-error">{errors.confirmPassword}</span>}
          </label>
        </div>
        <button className="btn-primary" disabled={loading}>{loading ? 'Creating…' : 'Create account'}</button>
      </form>
      <p className="muted">Already have an account? <Link to="/login">Sign in</Link></p>
    </div>
  )
}
