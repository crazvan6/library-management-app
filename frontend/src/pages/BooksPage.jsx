import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { searchBooks, listCategories } from '../api/catalogApi.js'
import { apiErrorMessage } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import Pagination from '../components/Pagination.jsx'

const DEFAULT_FILTERS = {
  title: '', author: '', categoryId: '', availableOnly: false, sortBy: 'title', sortDirection: 'ASC',
}
const PAGE_SIZE = 10

export default function BooksPage() {
  const { role } = useAuth()
  const isStaff = role === 'LIBRARIAN' || role === 'ADMIN'

  const [filters, setFilters] = useState(DEFAULT_FILTERS)   // live form state
  const [applied, setApplied] = useState(DEFAULT_FILTERS)   // committed search
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    listCategories().then(setCategories).catch(() => {})
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = { page, size: PAGE_SIZE, sortBy: applied.sortBy, sortDirection: applied.sortDirection }
      if (applied.title) params.title = applied.title
      if (applied.author) params.author = applied.author
      if (applied.categoryId) params.categoryId = applied.categoryId
      if (applied.availableOnly) params.availableOnly = true
      setData(await searchBooks(params))
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load books'))
    } finally {
      setLoading(false)
    }
  }, [page, applied])

  useEffect(() => { load() }, [load])

  const onFilterChange = (e) => {
    const { name, value, type, checked } = e.target
    setFilters((f) => ({ ...f, [name]: type === 'checkbox' ? checked : value }))
  }
  const onSearch = (e) => { e.preventDefault(); setApplied(filters); setPage(0) }
  const onReset = () => { setFilters(DEFAULT_FILTERS); setApplied(DEFAULT_FILTERS); setPage(0) }

  return (
    <div className="stack">
      <div className="toolbar">
        <h1>Catalog</h1>
        {isStaff && <Link className="btn-primary inline" to="/books/new">+ Add book</Link>}
      </div>

      <form className="card filters" onSubmit={onSearch}>
        <input name="title" placeholder="Title…" value={filters.title} onChange={onFilterChange} />
        <input name="author" placeholder="Author…" value={filters.author} onChange={onFilterChange} />
        <select name="categoryId" value={filters.categoryId} onChange={onFilterChange}>
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.categoryId} value={c.categoryId}>{c.name}</option>)}
        </select>
        <select name="sortBy" value={filters.sortBy} onChange={onFilterChange}>
          <option value="title">Sort: Title</option>
          <option value="author">Sort: Author</option>
          <option value="publicationYear">Sort: Year</option>
        </select>
        <select name="sortDirection" value={filters.sortDirection} onChange={onFilterChange}>
          <option value="ASC">Ascending</option>
          <option value="DESC">Descending</option>
        </select>
        <label className="checkbox">
          <input type="checkbox" name="availableOnly" checked={filters.availableOnly} onChange={onFilterChange} />
          Available only
        </label>
        <button className="btn-primary inline" type="submit">Search</button>
        <button className="btn-ghost" type="button" onClick={onReset}>Reset</button>
      </form>

      {error && <div className="alert error">{error}</div>}
      {loading && <p className="muted">Loading…</p>}

      {data && !loading && (
        <div className="card">
          <table className="table">
            <thead>
              <tr><th>Title</th><th>Author</th><th>ISBN</th><th>Available</th><th>Status</th><th>Categories</th></tr>
            </thead>
            <tbody>
              {data.content.length === 0 && (
                <tr><td colSpan="6" className="muted">No books match your search.</td></tr>
              )}
              {data.content.map((b) => (
                <tr key={b.bookId}>
                  <td><Link to={`/books/${b.bookId}`}>{b.title}</Link></td>
                  <td>{b.author}</td>
                  <td>{b.isbn}</td>
                  <td>{b.availableQuantity}</td>
                  <td><span className="badge">{b.status}</span></td>
                  <td>{(b.categoryNames || []).join(', ')}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={data.currentPage} totalPages={data.totalPages} totalElements={data.totalElements} onPage={setPage} />
        </div>
      )}
    </div>
  )
}
