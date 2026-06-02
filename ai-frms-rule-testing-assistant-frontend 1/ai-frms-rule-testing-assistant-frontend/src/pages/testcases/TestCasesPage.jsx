import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import Card from '../../components/common/Card'
import ErrorMessage from '../../components/common/ErrorMessage'
import Pagination from '../../components/common/Pagination'
import TestCaseTable from '../../components/testcases/TestCaseTable'
import { useToast } from '../../hooks/useToast'
import useClientPagination from '../../hooks/useClientPagination'
import testCaseService from '../../services/testCaseService'
import '../../styles/pages.css'

const TestCasesPage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()

  // All records loaded once from backend
  const [allTestCases, setAllTestCases] = useState([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState(null)
  const [deleting, setDeleting]         = useState(null)
  const [confirmId, setConfirmId]       = useState(null)

  // Client-side search filter
  const [search, setSearch] = useState('')

  // Load all test cases once on mount — no filter params
  const loadTestCases = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const data = await testCaseService.getAll()
      setAllTestCases(Array.isArray(data) ? data : [])
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { loadTestCases() }, [loadTestCases])

  // ── Client-side filtering ──────────────────────────────────────────────────
  const filteredTestCases = allTestCases.filter((t) => {
    if (search) {
      const q = search.toLowerCase()
      if (
        !(t.name        || '').toLowerCase().includes(q) &&
        !(t.description || '').toLowerCase().includes(q) &&
        !(t.ruleName    || '').toLowerCase().includes(q) &&
        !(t.scenarioName|| '').toLowerCase().includes(q)
      ) return false
    }
    return true
  })

  console.log('[TestCases] total', allTestCases.length, 'filtered', filteredTestCases.length)

  // ── Pagination ─────────────────────────────────────────────────────────────
  const {
    currentPage, pageSize, totalItems,
    paginatedItems: paginatedTestCases,
    setCurrentPage, setPageSize,
  } = useClientPagination(filteredTestCases)

  // Reset to page 1 whenever search changes
  useEffect(() => {
    setCurrentPage(1)
  }, [search]) // eslint-disable-line react-hooks/exhaustive-deps

  // ── Handlers ───────────────────────────────────────────────────────────────
  const handleDelete = async (id) => {
    setDeleting(id)
    try {
      await testCaseService.delete(id)
      setAllTestCases((prev) => prev.filter((t) => t.id !== id))
      addToast('Test case deleted', 'success')
    } catch (err) { addToast(err.message, 'error') }
    finally { setDeleting(null); setConfirmId(null) }
  }

  // ── Subtitle ───────────────────────────────────────────────────────────────
  const subtitleText = search
    ? `${filteredTestCases.length} of ${allTestCases.length} test case${allTestCases.length !== 1 ? 's' : ''} match`
    : `${allTestCases.length} test case${allTestCases.length !== 1 ? 's' : ''}`

  return (
    <div>
      <PageHeader
        title="Test Cases"
        subtitle={subtitleText}
        actions={<Button variant="primary" onClick={() => navigate('/testcases/create')}>+ Create Test Case</Button>}
      />

      <div className="rules-filters">
        <div className="rules-search-wrapper">
          <Input
            name="search"
            placeholder="Search by name, rule, or scenario…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {error ? (
        <ErrorMessage title="Failed to load test cases" message={error} onRetry={loadTestCases} />
      ) : (
        <>
          <Card noPadding>
            <TestCaseTable
              testCases={paginatedTestCases}
              loading={loading}
              onDelete={(id) => setConfirmId(id)}
              deletingId={deleting}
              confirmId={confirmId}
              onConfirmDelete={handleDelete}
              onCancelDelete={() => setConfirmId(null)}
            />
          </Card>

          {!loading && (
            <Pagination
              currentPage={currentPage}
              pageSize={pageSize}
              totalItems={totalItems}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          )}
        </>
      )}
    </div>
  )
}

export default TestCasesPage
