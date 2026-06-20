import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getBook, createBook, updateBook, listCategories, BOOK_STATUSES } from '../api/catalogApi.js'
import { apiErrorMessage } from '../api/client.js'

const EMPTY = { title: '', author: '', isbn: '', publicationYear: '', quantity: '', status: 'AVAILABLE', categoryIds: [] }

export default function BookFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState(EMPTY)
  const [categories, setCategories] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    listCategories().then(setCategories).catch(() => {})
  }, [])

  useEffect(() => {
    if (!isEdit) return
    getBook(id)
      .then((b) => setForm({
        title: b.title, author: b.author, isbn: b.isbn,
        publicationYear: b.publicationYear ?? '', quantity: b.quantity ?? '',
        status: b.status, categoryIds: (b.categories || []).map((c) => c.categoryId),
      }))
      .catch((e) => setError(apiErrorMessage(e, 'Book not found')))
  }, [id, isEdit])

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })
  const toggleCategory = (cid) =>
    setForm((f) => ({
      ...f,
      categoryIds: f.categoryIds.includes(cid) ? f.categoryIds.filter((x) => x !== cid) : [...f.categoryIds, cid],
    }))

  const onSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    if (!form.title.trim() || !form.author.trim() || form.categoryIds.length === 0) {
      setError('Title, author and at least one category are required')
      return
    }
    if (!isEdit && !form.isbn.trim()) { setError('ISBN is required'); return }
    setLoading(true)
    try {
      const payload = {
        title: form.title.trim(),
        author: form.author.trim(),
        publicationYear: form.publicationYear ? Number(form.publicationYear) : null,
        quantity: Number(form.quantity || 0),
        categoryIds: form.categoryIds,
      }
      if (isEdit) {
        payload.status = form.status
        await updateBook(id, payload)
        navigate(`/books/${id}`)
      } else {
        payload.isbn = form.isbn.trim()
        const created = await createBook(payload)
        navigate(`/books/${created.bookId}`)
      }
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not save the book'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-card" style={{ maxWidth: 560 }}>
      <h1>{isEdit ? 'Edit book' : 'Add book'}</h1>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={onSubmit} noValidate>
        <label>Title
          <input name="title" value={form.title} onChange={onChange} />
        </label>
        <label>Author
          <input name="author" value={form.author} onChange={onChange} />
        </label>
        {!isEdit && (
          <label>ISBN
            <input name="isbn" value={form.isbn} onChange={onChange} placeholder="e.g. 9780132350884" />
          </label>
        )}
        <div className="row2">
          <label>Publication year
            <input name="publicationYear" type="number" value={form.publicationYear} onChange={onChange} />
          </label>
          <label>Quantity
            <input name="quantity" type="number" min="0" value={form.quantity} onChange={onChange} />
          </label>
        </div>
        {isEdit && (
          <label>Status
            <select name="status" value={form.status} onChange={onChange}>
              {BOOK_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </label>
        )}
        <fieldset className="categories-field">
          <legend>Categories</legend>
          {categories.length === 0 && <p className="muted">No categories yet — create one first.</p>}
          {categories.map((c) => (
            <label key={c.categoryId} className="checkbox">
              <input type="checkbox" checked={form.categoryIds.includes(c.categoryId)} onChange={() => toggleCategory(c.categoryId)} />
              {c.name}
            </label>
          ))}
        </fieldset>
        <button className="btn-primary" disabled={loading}>{loading ? 'Saving…' : 'Save book'}</button>
      </form>
    </div>
  )
}
