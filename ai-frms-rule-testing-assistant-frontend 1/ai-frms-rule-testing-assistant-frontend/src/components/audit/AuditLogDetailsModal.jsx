import React from 'react'
import Modal from '../common/Modal'

const safeFormat = (val) => {
  if (val == null || val === '') return 'N/A'
  if (typeof val === 'object') return JSON.stringify(val, null, 2)
  const trimmed = String(val).trim()
  if (!trimmed) return 'N/A'
  try {
    const parsed = JSON.parse(trimmed)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return trimmed
  }
}

const JsonBlock = ({ label, value }) => (
  <div style={{ marginBottom: 16 }}>
    <p style={{
      margin: '0 0 6px',
      fontSize: 12,
      fontWeight: 600,
      color: 'var(--text-secondary)',
      textTransform: 'uppercase',
      letterSpacing: '0.5px',
    }}>
      {label}
    </p>
    <pre style={{
      margin: 0,
      padding: '10px 14px',
      background: 'var(--bg-secondary)',
      border: '1px solid var(--border)',
      borderRadius: 8,
      fontSize: 12,
      color: 'var(--text-primary)',
      fontFamily: 'monospace',
      whiteSpace: 'pre-wrap',
      wordBreak: 'break-word',
      maxHeight: 200,
      overflowY: 'auto',
    }}>
      {safeFormat(value)}
    </pre>
  </div>
)

const FieldRow = ({ label, value }) => (
  <div style={{ display: 'flex', gap: 12, marginBottom: 12, alignItems: 'flex-start' }}>
    <span style={{ fontSize: 13, color: 'var(--text-secondary)', minWidth: 110, flexShrink: 0 }}>
      {label}
    </span>
    <span style={{ fontSize: 13, color: 'var(--text-primary)', fontWeight: 500, wordBreak: 'break-word' }}>
      {value || '—'}
    </span>
  </div>
)

const AuditLogDetailsModal = ({ log, onClose }) => {
  if (!log) return null
  const ts = log.createdAt ? new Date(log.createdAt).toLocaleString() : '—'

  return (
    <Modal isOpen={!!log} title="Audit Log Details" onClose={onClose} size="lg">
      <FieldRow label="Audit ID"    value={log.auditId} />
      <FieldRow label="Timestamp"   value={ts} />
      <FieldRow label="Actor"       value={log.actor} />
      <FieldRow label="Action"      value={log.action} />
      <FieldRow label="Entity Type" value={log.entityType} />
      <FieldRow label="Entity Name" value={log.entityName} />
      <FieldRow label="Entity ID"   value={log.entityId} />
      {log.description && (
        <FieldRow label="Description" value={log.description} />
      )}
      <div style={{ borderTop: '1px solid var(--border)', paddingTop: 16, marginTop: 4 }}>
        <JsonBlock label="Old Value" value={log.oldValue} />
        <JsonBlock label="New Value" value={log.newValue} />
      </div>
    </Modal>
  )
}

export default AuditLogDetailsModal
