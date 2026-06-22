import client from '../api/client.js'

export async function loginRequest(email, password) {
  const { data } = await client.post('/v1/auth/login', { email, password })
  return data
}

export async function registerRequest(payload) {
  const { data } = await client.post('/v1/auth/register', payload)
  return data
}
