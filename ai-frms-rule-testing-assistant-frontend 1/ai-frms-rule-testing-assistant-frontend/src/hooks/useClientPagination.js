import { useState } from 'react'

function useClientPagination(items = [], initialPageSize = 10) {
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSizeState]  = useState(initialPageSize)

  const safeItems  = Array.isArray(items) ? items : []
  const totalItems = safeItems.length
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))

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
