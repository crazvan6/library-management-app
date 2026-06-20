import { useEffect, useState } from 'react'
import { listCategories, createCategory, updateCategory, deleteCategory } from '../api/catalogApi.js'
import { apiErrorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'

export default function CategoriesPage() {
  const { role } = useAuth()
  const isAdmin = role === 'ADMIN'

  const [categories, setCategories] = useState([])
  const [form, setForm] = useState({ name: '', description: '' })
  const [editingId, setEditingId] = useState(null)
  const [error, setError] = useState(null)

  const load = () => listCategories().then(setCategories).catch((e) => setError(apiErrorMessage(e)))
  useEffect(() => { load() }, [])

  const onSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    if (!form.name.trim()) { setError('Name is required'); return }
    try {
      if (editingId) await updateCategory(editingId, form)
      else await createCategory(form)
      setForm({ name: '', description: '' })
      setEditingId(null)
      load()
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save category'))
    }
  }

  const startEdit = (c) => { setEditingId(c.categoryId); setForm({ name: c.name, description: c.description || '' }) }
  const cancelEdit = () => { setEditingId(null); setForm({ name: '', description: '' }) }

  const onDelete = async (c) => {
    if (!window.confirm(`Delete category "${c.name}"?`)) return
    try {
      await deleteCategory(c.categoryId)
      load()
    } catch (err) {
      setError(apiErrorMessage(err, 'Delete failed — category may still have books'))
    }
  }

  return (
    <div className="stack">
      <h1>Categories</h1>
      {error && <div className="alert error">{error}</div>}

      <form className="card filters" onSubmit={onSubmit}>
        <input name="name" placeholder="Category name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <input name="description" placeholder="Description (optional)" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <button className="btn-primary inline" type="submit">{editingId ? 'Update' : 'Add'}</button>
        {editingId && <button className="btn-ghost" type="button" onClick={cancelEdit}>Cancel</button>}
      </form>

      <div className="card">
        <table className="table">
          <thead><tr><th>Name</th><th>Description</th><th>Books</th><th></th></tr></thead>
          <tbody>
            {categories.length === 0 && <tr><td colSpan="4" className="muted">No categories yet.</td></tr>}
            {categories.map((c) => (
              <tr key={c.categoryId}>
                <td>{c.name}</td>
                <td className="muted">{c.description || '—'}</td>
                <td>{c.bookCount}</td>
                <td className="actions">
                  <button className="btn-ghost" onClick={() => startEdit(c)}>Edit</button>
                  {isAdmin && <button className="btn-danger" onClick={() => onDelete(c)}>Delete</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
