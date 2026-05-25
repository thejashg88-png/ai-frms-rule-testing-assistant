import React, { useState, useEffect } from 'react'
import Select from '../common/Select'
import Button from '../common/Button'
import AiResponseBox from './AiResponseBox'
import executionService from '../../services/executionService'
import aiService from '../../services/aiService'

const AiFailureAnalysis = () => {
  const [executions, setExecutions] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    executionService.getAll({ status: 'FAILED' }).then(setExecutions).catch(() => {})
  }, [])

  const options = executions.map((e) => ({ value: String(e.id), label: `#${e.id} — ${e.entityName}` }))

  const handleAnalyze = async () => {
    if (!selectedId) return
    setLoading(true); setResponse(null); setError(null)
    try {
      const execution = executions.find((e) => e.id === Number(selectedId))
      const res = await aiService.analyzeFailure(execution)
      setResponse(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const formatContent = (res) => {
    if (!res) return null
    const suggestions = res.suggestions?.map((s, i) => `${i + 1}. ${s}`).join('\n')
    return `Summary: ${res.summary}\n\nRoot Cause: ${res.rootCause}\n\nSuggestions:\n${suggestions}\n\nConfidence: ${Math.round((res.confidence ?? 0) * 100)}%`
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', marginBottom: 16 }}>
        <div style={{ flex: 1 }}>
          <Select label="Select Failed Execution" name="execution" placeholder="Choose a failed execution…"
            options={options} value={selectedId} onChange={(e) => { setSelectedId(e.target.value); setResponse(null) }} />
        </div>
        <div style={{ paddingBottom: 16 }}>
          <Button variant="primary" onClick={handleAnalyze} loading={loading} disabled={!selectedId}>
            Analyze Failure
          </Button>
        </div>
      </div>

      <AiResponseBox loading={loading} error={error} content={formatContent(response)} />
    </div>
  )
}

export default AiFailureAnalysis
