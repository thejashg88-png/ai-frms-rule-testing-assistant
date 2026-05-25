import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { ShieldCheck, Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { useToast } from '../../hooks/useToast'
import Input from '../../components/common/Input'
import Button from '../../components/common/Button'
import '../../styles/pages.css'

const LoginPage = () => {
  const [email, setEmail]               = useState('')
  const [password, setPassword]         = useState('')
  const [loading, setLoading]           = useState(false)
  const [errors, setErrors]             = useState({})
  const [apiError, setApiError]         = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const { login }    = useAuth()
  const navigate     = useNavigate()
  const { addToast } = useToast()

  const validate = () => {
    const errs = {}
    if (!email.trim())    errs.email    = 'Email is required'
    if (!password.trim()) errs.password = 'Password is required'
    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }

    setLoading(true)
    setApiError('')
    try {
      const result = await login(email, password)
      if (result.success) {
        addToast('Login successful!', 'success')
        navigate('/dashboard')
      } else {
        setApiError(result.error || 'Invalid email or password')
      }
    } catch {
      setApiError('An unexpected error occurred')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 8,
            background: '#dbeafe',
            color: '#2563eb',
            borderRadius: 20,
            padding: '6px 14px',
            fontSize: 13,
            fontWeight: 600,
            marginBottom: 16,
          }}>
            <ShieldCheck size={16} />
            AI FRMS Rule Testing
          </div>
          <h1 className="login-title">Welcome Back</h1>
          <p className="login-subtitle">Sign in to continue to the dashboard</p>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <Input
            label="Email"
            type="email"
            placeholder="your@email.com"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value)
              setErrors((p) => ({ ...p, email: '' }))
              setApiError('')
            }}
            error={errors.email}
            required
          />

          <div className="input-wrapper">
            <label className="input-label">
              Password<span className="required">*</span>
            </label>
            <div className="password-field-wrap">
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter your password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  setErrors((p) => ({ ...p, password: '' }))
                  setApiError('')
                }}
                className={`input ${errors.password ? 'input-error' : ''}`}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword((v) => !v)}
                tabIndex={-1}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {errors.password && <span className="error-text">{errors.password}</span>}
          </div>

          {apiError && (
            <div className="auth-api-error" role="alert">
              {apiError}
            </div>
          )}

          <Button
            type="submit"
            variant="primary"
            size="lg"
            loading={loading}
            disabled={loading}
            className="login-button"
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </Button>
        </form>

        <p className="signup-signin-link" style={{ marginTop: 16 }}>
          Don't have an account?{' '}
          <Link to="/signup">Sign Up</Link>
        </p>

        {import.meta.env.VITE_ENABLE_MOCK_LOGIN === 'true' && (
          <p className="login-footer">
            Demo mode — any email &amp; password will work
          </p>
        )}
      </div>
    </div>
  )
}

export default LoginPage
