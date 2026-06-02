import React, { useState, useRef, useEffect } from 'react'
import Button from '../common/Button'
import Loader from '../common/Loader'
import aiService from '../../services/aiService'

const SAMPLE_QUESTIONS = [
  'Why did my High Frequency Transaction test fail?',
  'How do I test SEQUENTIAL_TXN?',
  'What data is needed for UNUSUAL_AMT?',
  'Explain difference between payment status and risk action.',
  'How do I create test cases for STRUCTURING?',
]

const formatTime = (date) =>
  date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })

const renderMessageContent = (content) => {
  if (content === null || content === undefined) return ''
  if (typeof content === 'string') return content
  if (typeof content === 'object') {
    return content.reply || content.answer || content.response || JSON.stringify(content, null, 2)
  }
  return String(content)
}

const AiChat = () => {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const messagesEndRef = useRef(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const sendMessage = async (text) => {
    const messageText = (text || input).trim()
    if (!messageText) return

    const userMessage = {
      role: 'user',
      content: messageText,
      timestamp: new Date(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInput('')
    setLoading(true)
    setError(null)

    try {
      const result = await aiService.chat({
        message: messageText,
        context: { source: 'AI-FRMS Rule Testing Assistant' },
      })

      console.log('[AI Chat Raw Response]', result?.raw)
      console.log('[AI Chat Normalized Response]', result)

      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: result.reply,
          timestamp: new Date(),
        },
      ])
    } catch (err) {
      setError('AI chat failed. Please check backend/AI service.')
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  const handleClear = () => {
    setMessages([])
    setError(null)
  }

  const showSampleQuestions = messages.length === 0

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 540 }}>

      {/* Subheader row */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <p style={{ margin: 0, fontSize: 13, color: 'var(--text-secondary)' }}>
          Ask questions about fraud rules, test cases, executions, or debugging.
        </p>
        {messages.length > 0 && (
          <Button variant="secondary" size="sm" onClick={handleClear}>
            Clear Chat
          </Button>
        )}
      </div>

      {/* Sample question chips */}
      {showSampleQuestions && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
          {SAMPLE_QUESTIONS.map((q, i) => (
            <button
              key={i}
              onClick={() => sendMessage(q)}
              style={{
                padding: '6px 12px',
                border: '1px solid var(--border)',
                borderRadius: 20,
                background: 'var(--bg-secondary)',
                color: 'var(--text-secondary)',
                fontSize: 12,
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = 'var(--primary)'
                e.currentTarget.style.color = 'var(--primary)'
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = 'var(--border)'
                e.currentTarget.style.color = 'var(--text-secondary)'
              }}
            >
              {q}
            </button>
          ))}
        </div>
      )}

      {/* Messages area */}
      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '4px 2px',
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
        }}
      >
        {messages.length === 0 && !loading && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
            <p style={{ color: 'var(--text-secondary)', fontSize: 14, textAlign: 'center', margin: 0 }}>
              Select a question above or type your own to get started.
            </p>
          </div>
        )}

        {messages.map((msg, idx) => (
          <div
            key={idx}
            style={{
              display: 'flex',
              justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
            }}
          >
            <div
              style={{
                maxWidth: '75%',
                padding: '10px 14px',
                borderRadius: msg.role === 'user'
                  ? '18px 18px 4px 18px'
                  : '18px 18px 18px 4px',
                background: msg.role === 'user'
                  ? 'var(--primary)'
                  : 'var(--color-ai-response-bg, var(--bg-secondary))',
                color: msg.role === 'user' ? '#fff' : 'var(--text-primary)',
                border: msg.role === 'user'
                  ? 'none'
                  : '1px solid var(--color-ai-border, var(--border))',
                fontSize: 14,
                lineHeight: 1.6,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
              }}
            >
              {renderMessageContent(msg.content)}
              <div
                style={{
                  fontSize: 11,
                  marginTop: 4,
                  opacity: 0.55,
                  textAlign: msg.role === 'user' ? 'right' : 'left',
                  color: msg.role === 'user' ? '#fff' : 'var(--text-secondary)',
                }}
              >
                {formatTime(msg.timestamp)}
              </div>
            </div>
          </div>
        ))}

        {loading && (
          <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
            <div
              style={{
                padding: '10px 16px',
                borderRadius: '18px 18px 18px 4px',
                background: 'var(--color-ai-response-bg, var(--bg-secondary))',
                border: '1px solid var(--color-ai-border, var(--border))',
              }}
            >
              <Loader size="sm" message="AI is thinking…" />
            </div>
          </div>
        )}

        {error && (
          <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
            <div
              style={{
                padding: '10px 14px',
                borderRadius: '18px 18px 18px 4px',
                background: 'var(--bg-secondary)',
                border: '1px solid var(--danger)',
                fontSize: 13,
                color: 'var(--danger)',
              }}
            >
              {error}
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input row */}
      <div
        style={{
          display: 'flex',
          gap: 8,
          paddingTop: 12,
          borderTop: '1px solid var(--border)',
          alignItems: 'flex-end',
        }}
      >
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask a question about fraud rules, test cases, or executions…"
          rows={2}
          disabled={loading}
          style={{
            flex: 1,
            padding: '10px 14px',
            border: '1px solid var(--border)',
            borderRadius: 10,
            background: 'var(--bg-secondary)',
            color: 'var(--text-primary)',
            fontSize: 14,
            resize: 'none',
            outline: 'none',
            lineHeight: 1.5,
            fontFamily: 'inherit',
          }}
        />
        <Button
          variant="primary"
          onClick={() => sendMessage()}
          loading={loading}
          disabled={!input.trim() || loading}
        >
          Send
        </Button>
      </div>

      <p style={{ margin: '6px 0 0', fontSize: 11, color: 'var(--text-secondary)' }}>
        Press Enter to send · Shift + Enter for new line
      </p>
    </div>
  )
}

export default AiChat
