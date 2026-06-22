import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getStoredAuth, storeAuth, setUnauthorizedHandler } from '../api/client.js'
import { loginRequest, registerRequest } from './authApi.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => getStoredAuth())
  const navigate = useNavigate()

  // When a request returns 401, drop the session and send the user to login.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setAuth(null)
      navigate('/login', { replace: true })
    })
  }, [navigate])

  const login = async (email, password) => {
    const data = await loginRequest(email, password)
    storeAuth(data)
    setAuth(data)
    return data
  }

  const register = async (payload) => {
    const data = await registerRequest(payload)
    storeAuth(data)
    setAuth(data)
    return data
  }

  const logout = () => {
    storeAuth(null)
    setAuth(null)
    navigate('/login', { replace: true })
  }

  const value = useMemo(
    () => ({
      user: auth,
      isAuthenticated: !!auth?.token,
      role: auth?.role,
      login,
      register,
      logout,
    }),
    [auth], // eslint-disable-line react-hooks/exhaustive-deps
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
