import React, { useState } from 'react'
import PageHeader from '../../components/common/PageHeader'
import Card from '../../components/common/Card'
import Input from '../../components/common/Input'
import Select from '../../components/common/Select'
import Button from '../../components/common/Button'
import { useToast } from '../../hooks/useToast'

const THEME_OPTIONS = [
  { value: 'light', label: 'Light' },
  { value: 'dark',  label: 'Dark (coming soon)' },
]

const SettingsPage = () => {
  const { addToast } = useToast()
  const [settings, setSettings] = useState({
    appName: import.meta.env.VITE_APP_NAME ?? 'AI FRMS',
    apiUrl: import.meta.env.VITE_API_BASE_URL ?? '',
    theme: 'light',
    pageSize: '20',
  })

  const set = (e) => setSettings((p) => ({ ...p, [e.target.name]: e.target.value }))

  const handleSave = () => {
    addToast('Settings saved (display only — no backend persistence in this version)', 'success')
  }

  return (
    <div>
      <PageHeader title="Settings" subtitle="Application configuration" />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        <Card title="Application" subtitle="General application settings">
          <Input label="App Name" name="appName" value={settings.appName} onChange={set} />
          <Input label="API Base URL" name="apiUrl" value={settings.apiUrl} onChange={set} helperText="Backend API URL — set in .env file" />
          <Select label="Theme" name="theme" options={THEME_OPTIONS} value={settings.theme} onChange={set} />
          <Input label="Default Page Size" name="pageSize" type="number" value={settings.pageSize} onChange={set} />
          <Button variant="primary" onClick={handleSave}>Save Settings</Button>
        </Card>

        <Card title="Environment" subtitle="Current environment information">
          <div style={{ display: 'grid', gap: 8 }}>
            {[
              ['App Environment', import.meta.env.VITE_APP_ENV],
              ['API Base URL', import.meta.env.VITE_API_BASE_URL],
              ['Mock Mode', import.meta.env.VITE_ENABLE_MOCK_LOGIN === 'true' ? 'Enabled' : 'Disabled'],
              ['Backend URL', import.meta.env.VITE_BACKEND_BASE_URL],
            ].map(([label, value]) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
                <span style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>{label}</span>
                <code style={{ color: 'var(--text-primary)', background: 'var(--bg-secondary)', padding: '2px 8px', borderRadius: 4 }}>{value ?? '—'}</code>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  )
}

export default SettingsPage
