// Reusable pagination control driven by the backend PageResponse envelope.
export default function Pagination({ page, totalPages, totalElements, onPage }) {
  if (!totalPages || totalPages <= 1) {
    return <p className="muted pagination-summary">{totalElements ?? 0} result(s)</p>
  }
  return (
    <div className="pagination">
      <button className="btn-ghost" disabled={page <= 0} onClick={() => onPage(page - 1)}>← Prev</button>
      <span className="muted">Page {page + 1} of {totalPages} · {totalElements} result(s)</span>
      <button className="btn-ghost" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>Next →</button>
    </div>
  )
}
