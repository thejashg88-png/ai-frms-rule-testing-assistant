import { useState } from 'react'

// Client-side pagination hook. Accepts the FULL (already-filtered) items array
// and slices it for the current page.
//
// Important: filtering must be applied BEFORE passing items here.
// currentPage is 1-based (page 1 = first page). If server-side pagination is
// added later, this hook should be replaced with a server-aware version.
//
// Changing pageSize always resets to page 1 to avoid an empty page state.
function useClientPagination(items = [], initialPageSize = 10) {
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSizeState]  = useState(initialPageSize)

  const safeItems  = Array.isArray(items) ? items : []
  const totalItems = safeItems.length
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))

  // Clamp currentPage so it never exceeds totalPages after a filter reduces the result set.
  const safeCurrentPage = Math.min(currentPage, totalPages)
  const startIndex      = (safeCurrentPage - 1) * pageSize
  const endIndex        = Math.min(startIndex + pageSize, totalItems)
  const paginatedItems  = safeItems.slice(startIndex, endIndex)

  const setPageSize = (newSize) => {
    setPageSizeState(Number(newSize))
    setCurrentPage(1)
  }

  const resetPage = () => setCurrentPage(1)

  return {
    currentPage: safeCurrentPage,
    pageSize,
    totalItems,
    totalPages,
    startIndex,
    endIndex,
    paginatedItems,
    setCurrentPage,
    setPageSize,
    resetPage,
  }
}

export default useClientPagination
