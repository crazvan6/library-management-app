import client from './client.js'

// ---- Books ----
export async function searchBooks(params) {
  const { data } = await client.get('/v1/books/search', { params })
  return data
}

export async function getBook(id) {
  const { data } = await client.get(`/v1/books/${id}`)
  return data
}

export async function createBook(payload) {
  const { data } = await client.post('/v1/books', payload)
  return data
}

export async function updateBook(id, payload) {
  const { data } = await client.put(`/v1/books/${id}`, payload)
  return data
}

export async function deleteBook(id) {
  await client.delete(`/v1/books/${id}`)
}

// ---- Categories ----
export async function listCategories() {
  const { data } = await client.get('/v1/categories')
  return data
}

export async function createCategory(payload) {
  const { data } = await client.post('/v1/categories', payload)
  return data
}

export async function updateCategory(id, payload) {
  const { data } = await client.put(`/v1/categories/${id}`, payload)
  return data
}

export async function deleteCategory(id) {
  await client.delete(`/v1/categories/${id}`)
}

export const BOOK_STATUSES = ['AVAILABLE', 'MAINTENANCE', 'DISCONTINUED']
