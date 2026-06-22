// Client-side validators mirroring the backend Bean Validation rules.

export function validateEmail(email) {
  if (!email) return 'Email is required'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Enter a valid email address'
  return null
}

export function validatePassword(password) {
  if (!password) return 'Password is required'
  if (password.length < 8) return 'At least 8 characters'
  if (!/[A-Z]/.test(password)) return 'At least one uppercase letter'
  if (!/[a-z]/.test(password)) return 'At least one lowercase letter'
  if (!/\d/.test(password)) return 'At least one digit'
  if (!/[!@#$%^&*()_+\-[\]{}|;:,.<>?]/.test(password)) return 'At least one special character'
  return null
}

export function validateRequired(value, label) {
  return value && String(value).trim() ? null : `${label} is required`
}

// Returns the first non-null error from a map of { field: errorOrNull }.
export function firstError(errors) {
  return Object.values(errors).find((e) => e) || null
}
