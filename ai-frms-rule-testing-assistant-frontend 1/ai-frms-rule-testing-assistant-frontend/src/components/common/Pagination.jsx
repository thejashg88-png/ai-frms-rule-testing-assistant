import React from 'react'
import Button from './Button'
import Select from './Select'

const DEFAULT_PAGE_SIZE_OPTIONS = [
  { value: '10', label: '10 per page' },
  { value: '20', label: '20 per page' },
  { value: '50', label: '50 per page' },
]

const Pagination = ({
  currentPage,
  pageSize,
  totalItems,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
}) => {
  if (totalItems === 0) return null

  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
  const rangeStart = (currentPage - 1) * pageSize + 1
  const rangeEnd   = Math.min(currentPage * pageSize, totalItems)

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      flexWrap: 'wrap',
      gap: 12,
      marginTop: 16,
      padding: '12px 16px',
      background: 'var(--bg-card)',
      border: '1px solid var(--border)',
      borderRadius: 10,
    }}>
      {/* Record range */}
      <span style={{ fontSize: 13, color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
        Showing {rangeStart}–{rangeEnd} of {totalItems} record{totalItems !== 1 ? 's' : ''}
      </span>

      {/* Prev / page label / Next */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.max(1, currentPage - 1))}
          disabled={currentPage <= 1}
        >
          ← Previous
        </Button>
        <span style={{
          fontSize: 13,
          color: 'var(--text-primary)',
          whiteSpace: 'nowrap',
          minWidth: 90,
          textAlign: 'center',
        }}>
          Page {currentPage} of {totalPages}
        </span>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
          disabled={currentPage >= totalPages}
        >
          Next →
        </Button>
      </div>

      {/* Page size */}
      <div style={{ minWidth: 130 }}>
        <Select
          name="pageSize"
          options={pageSizeOptions}
          value={String(pageSize)}
          onChange={(e) => onPageSizeChange(Number(e.target.value))}
        />
      </div>
    </div>
  )
}

export default Pagination
