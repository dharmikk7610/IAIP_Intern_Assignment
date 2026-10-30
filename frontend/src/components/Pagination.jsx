const PAGE_SIZES = [5, 10, 20]

/** Page numbers to show: first, last, and up to 2 around the current page, with gaps as "…". */
function visiblePages(current, total) {
  const pages = new Set([1, total, current - 1, current, current + 1].filter((p) => p >= 1 && p <= total))
  const sorted = [...pages].sort((a, b) => a - b)
  const result = []
  sorted.forEach((page, i) => {
    if (i > 0 && page - sorted[i - 1] > 1) result.push('gap-' + page)
    result.push(page)
  })
  return result
}

export default function Pagination({ pageNumber, pageSize, totalElements, totalPages, onPageChange, onPageSizeChange }) {
  const from = totalElements === 0 ? 0 : (pageNumber - 1) * pageSize + 1
  const to = Math.min(pageNumber * pageSize, totalElements)

  return (
    <div className="pagination">
      <p className="pagination-summary">
        Showing <strong>{from}</strong>–<strong>{to}</strong> of <strong>{totalElements}</strong> records
      </p>
      <div className="pagination-controls">
        <label className="page-size">
          Rows per page
          <select value={pageSize} onChange={(e) => onPageSizeChange(Number(e.target.value))}>
            {PAGE_SIZES.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </label>
        <nav className="pager" aria-label="Pagination">
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={pageNumber <= 1}
            onClick={() => onPageChange(pageNumber - 1)}
          >
            ‹ Prev
          </button>
          {totalPages > 0 &&
            visiblePages(pageNumber, totalPages).map((page) =>
              typeof page === 'string' ? (
                <span key={page} className="pager-gap">
                  …
                </span>
              ) : (
                <button
                  key={page}
                  type="button"
                  className={`btn btn-sm ${page === pageNumber ? 'btn-primary' : 'btn-ghost'}`}
                  aria-current={page === pageNumber ? 'page' : undefined}
                  onClick={() => onPageChange(page)}
                >
                  {page}
                </button>
              ),
            )}
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={pageNumber >= totalPages}
            onClick={() => onPageChange(pageNumber + 1)}
          >
            Next ›
          </button>
        </nav>
      </div>
    </div>
  )
}
