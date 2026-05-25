import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Shield, CheckCircle, Lock, UserCheck, BarChart3, Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { useToast } from '../../hooks/useToast'
import Input from '../../components/common/Input'
import Button from '../../components/common/Button'
import '../../styles/pages.css'

const FEATURES = [
  { icon: Shield,      text: 'AI-Powered Rule Testing' },
  { icon: BarChart3,   text: 'Real-Time Fraud Detection' },
  { icon: UserCheck,   text: 'Advanced Risk Analytics' },
  { icon: Lock,        text: 'Comprehensive Audit Trails' },
]

const SignUpPage = () => {
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
  })
  const [errors, setErrors]                       = useState({})
  const [loading, setLoading]                     = useState(false)
  const [success, setSuccess]                     = useState(false)
  const [apiError, setApiError]                   = useState('')
  const [showPassword, setShowPassword]           = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  const { register }   = useAuth()
  const navigate       = useNavigate()
  const { addToast }   = useToast()

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }))
    setErrors((prev) => ({ ...prev, [field]: '' }))
    setApiError('')
  }

  const validate = () => {
    const errs = {}
    if (!form.fullName.trim())   errs.fullName = 'Full name is required'
    if (!form.email.trim())      errs.email    = 'Email is required'
    else if (!/\S+@\S+\.\S+/.test(form.email)) errs.email = 'Enter a valid email address'
    if (!form.username.trim())   errs.username = 'Username is required'
    if (!form.password)          errs.password = 'Password is required'
    else if (form.password.length < 8) errs.password = 'Password must be at least 8 characters'
    if (!form.confirmPassword)         errs.confirmPassword = 'Please confirm your password'
    else if (form.password !== form.confirmPassword) errs.confirmPassword = 'Passwords do not match'
    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length > 0) { setErrors(errs); return }

    setLoading(true)
    setApiError('')
    try {
      const result = await register({
        fullName: form.fullName,
        email:    form.email,
        username: form.username,
        password: form.password,
      })
      if (result.success) {
        setSuccess(true)
        addToast('Account created! Redirecting to sign in…', 'success')
        setTimeout(() => navigate('/login'), 2200)
      } else {
        setApiError(result.error || 'Registration failed. Please check the details and try again.')
      }
    } catch {
      setApiError('An unexpected error occurred')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="signup-page">

      {/* ── Left brand panel ── */}
      <div className="signup-brand">
        <div className="signup-brand-inner">

          <div className="signup-shield-icon">
            <Shield size={52} strokeWidth={1.4} />
          </div>

          <div className="signup-brand-titles">
            <h1 className="signup-product-name">AegisFRM AI</h1>
            <p className="signup-tagline">Fraud &amp; Risk Intelligence</p>
          </div>

          <div className="signup-brand-divider" />

          <ul className="signup-feature-list">
            {FEATURES.map(({ icon: Icon, text }) => (
              <li key={text}>
                <span className="signup-feature-icon"><Icon size={15} /></span>
                {text}
              </li>
            ))}
          </ul>

          <p className="signup-brand-footer">
            Trusted by fraud &amp; risk teams worldwide
          </p>
        </div>
      </div>

      {/* ── Right form panel ── */}
      <div className="signup-form-panel">
        <div className="signup-form-inner">

          {success ? (
            <div className="signup-success-state">
              <CheckCircle size={52} className="signup-success-icon" />
              <h2 className="signup-success-title">Account Created!</h2>
              <p className="signup-success-sub">Redirecting you to sign in…</p>
            </div>
          ) : (
            <>
              <h2 className="signup-form-title">Create your account</h2>
              <p className="signup-form-subtitle">Join AegisFRM AI to get started</p>

              <form onSubmit={handleSubmit} noValidate>
                <Input
                  label="Full Name"
                  placeholder="John Doe"
                  value={form.fullName}
                  onChange={handleChange('fullName')}
                  error={errors.fullName}
                  required
                />
                <Input
                  label="Email"
                  type="email"
                  placeholder="john@example.com"
                  value={form.email}
                  onChange={handleChange('email')}
                  error={errors.email}
                  required
                />
                <Input
                  label="Username"
                  placeholder="johndoe"
                  value={form.username}
                  onChange={handleChange('username')}
                  error={errors.username}
                  required
                />

                {/* Password with show/hide toggle */}
                <div className="input-wrapper">
                  <label className="input-label">
                    Password<span className="required">*</span>
                  </label>
                  <div className="password-field-wrap">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      placeholder="Min. 8 characters"
                      value={form.password}
                      onChange={handleChange('password')}
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

                {/* Confirm Password with independent show/hide toggle */}
                <div className="input-wrapper">
                  <label className="input-label">
                    Confirm Password<span className="required">*</span>
                  </label>
                  <div className="password-field-wrap">
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      placeholder="Re-enter your password"
                      value={form.confirmPassword}
                      onChange={handleChange('confirmPassword')}
                      className={`input ${errors.confirmPassword ? 'input-error' : ''}`}
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowConfirmPassword((v) => !v)}
                      tabIndex={-1}
                      aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                    >
                      {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  {errors.confirmPassword && <span className="error-text">{errors.confirmPassword}</span>}
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
                  className="signup-submit-btn"
                >
                  {loading ? 'Creating Account…' : 'Create Account'}
                </Button>
              </form>

              <p className="signup-signin-link">
                Already have an account?{' '}
                <Link to="/login">Sign In</Link>
              </p>

              {import.meta.env.VITE_ENABLE_MOCK_LOGIN === 'true' && (
                <p className="login-footer">Demo mode — signup is simulated</p>
              )}
            </>
          )}

        </div>
      </div>
    </div>
  )
}

export default SignUpPage
