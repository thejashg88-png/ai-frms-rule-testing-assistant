import { useState, useCallback } from 'react'

export const usePagination = (initialPage = 1, initialPageSize = 10) => {
  const [currentPage, setCurrentPage] = useState(initialPage)
  const [pageSize, setPageSize] = useState(initialPageSize)
  const [totalItems, setTotalItems] = useState(0)

  const totalPages = Math.ceil(totalItems / pageSize)

  const goToPage = useCallback((page) => {
    const pageNum = Math.max(1, Math.min(page, totalPages))
    setCurrentPage(pageNum)
  }, [totalPages])

  const nextPage = useCallback(() => {
    goToPage(currentPage + 1)
  }, [currentPage, goToPage])

  const prevPage = useCallback(() => {
    goToPage(currentPage - 1)
  }, [currentPage, goToPage])

  const changePageSize = useCallback((newPageSize) => {
    setPageSize(newPageSize)
    setCurrentPage(1)
  }, [])

  return {
    currentPage,
    pageSize,
    totalItems,
    totalPages,
    goToPage,
    nextPage,
    prevPage,
    changePageSize,
    setTotalItems,
  }
}

export default usePagination