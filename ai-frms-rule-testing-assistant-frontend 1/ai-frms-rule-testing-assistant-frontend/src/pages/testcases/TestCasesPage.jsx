import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import PageHeader from '../../components/common/PageHeader'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import Card from '../../components/common/Card'
import ErrorMessage from '../../components/common/ErrorMessage'
import TestCaseTable from '../../components/testcases/TestCaseTable'
import { useToast } from '../../hooks/useToast'
import testCaseService from '../../services/testCaseService'
import '../../styles/pages.css'

const TestCasesPage = () => {
  const navigate = useNavigate()
  const { addToast } = useToast()

  const [testCases, setTestCases] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [confirmId, setConfirmId] = useState(null)
  const [search, setSearch] = useState('')

  const loadTestCases = useCallback(async () => {
    setLoading(true); setError(null)
    try { setTestCases(await testCaseService.getAll({ search })) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }, [search])

  useEffect(() => { loadTestCases() }, [loadTestCases])

  const handleDelete = async (id) => {
    setDeleting(id)
    try {
      await testCaseService.delete(id)
      setTestCases((prev) => prev.filter((t) => t.id !== id))
      addToast('Test case deleted', 'success')
    } catch (err) { addToast(err.message, 'error') }
    finally { setDeleting(null); setConfirmId(null) }
  }

  return (
    <div>
      <PageHeader
        title="Test Cases"
        subtitle={`${testCases.length} test case${testCases.length !== 1 ? 's' : ''}`}
        actions={<Button variant="primary" onClick={() => navigate('/testcases/create')}>+ Create Test Case</Button>}
      />

      <div className="rules-filters">
        <div className="rules-search-wrapper">
          <Input name="search" placeholder="Search test cases…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      {error ? (
        <ErrorMessage title="Failed to load test cases" message={error} onRetry={loadTestCases} />
      ) : (
        <Card noPadding>
          <TestCaseTable
            testCases={testCases}
            loading={loading}
            onDelete={(id) => setConfirmId(id)}
            deletingId={deleting}
            confirmId={confirmId}
            onConfirmDelete={handleDelete}
            onCancelDelete={() => setConfirmId(null)}
          />
        </Card>
      )}
    </div>
  )
}

export default TestCasesPage
