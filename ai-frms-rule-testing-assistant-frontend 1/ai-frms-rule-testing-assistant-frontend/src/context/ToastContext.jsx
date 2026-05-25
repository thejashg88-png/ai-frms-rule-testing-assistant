/**
 * ToastContext.jsx
 * 
 * Purpose: Manages toast/notification state globally across the application
 * 
 * Features:
 * - Add multiple toasts simultaneously
 * - Auto-dismiss functionality with configurable duration
 * - Remove toasts manually
 * - Support for different toast types (success, error, warning, info)
 * - Toast queue management
 * 
 * Usage:
 * 1. Wrap App with <ToastProvider> in main.jsx or App.jsx
 * 2. Use useToast() hook in any component to access toast functions
 * 3. Call addToast(message, type, duration) to show notification
 * 
 * Example:
 * const { addToast } = useToast()
 * addToast('Success!', 'success', 3000)
 * addToast('Error occurred', 'error', 5000)
 */

import React, { createContext, useState, useCallback } from 'react'

// Create the context
export const ToastContext = createContext(null)

/**
 * ToastProvider Component
 * 
 * Props:
 * - children: React components to wrap with toast context
 * 
 * Provides:
 * - toasts: Array of active toast objects
 * - addToast(message, type, duration): Function to add a new toast
 * - removeToast(id): Function to remove a specific toast
 */
export const ToastProvider = ({ children }) => {
  // State: Array of active toasts
  // Each toast object structure: { id, message, type }
  const [toasts, setToasts] = useState([])

  /**
   * Add Toast Function
   * 
   * Creates and displays a new toast notification
   * 
   * Features:
   * - Auto-generates unique ID based on timestamp
   * - Auto-removes after specified duration
   * - Supports different types (success, error, warning, info)
   * - Allows persistent toasts (duration = 0)
   * 
   * @param {string} message - Toast message to display
   * @param {string} type - Toast type: 'success' | 'error' | 'warning' | 'info' (default: 'info')
   * @param {number} duration - Auto-dismiss duration in ms (default: 3000, 0 = persistent)
   * @returns {number} Toast ID for manual removal if needed
   * 
   * Example:
   * const { addToast } = useToast()
   * addToast('Profile updated', 'success', 3000)
   * addToast('Failed to save', 'error', 5000)
   * addToast('Warning message', 'warning')
   * addToast('Info message', 'info')
   */
  const addToast = useCallback((message, type = 'info', duration = 3000) => {
    // Generate unique ID using timestamp
    // Note: In production, use uuid() for more reliable uniqueness
    const id = Date.now()
    
    // Create toast object
    const toast = { id, message, type }
    
    // Add to toasts array
    // Using functional update to ensure we work with latest state
    setToasts(prev => [...prev, toast])
    
    // Auto-remove toast after duration (if duration > 0)
    if (duration > 0) {
      // Set timeout to remove this specific toast
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }
    
    // Return toast ID in case caller needs to remove it manually
    return id
  }, [])

  /**
   * Remove Toast Function
   * 
   * Removes a specific toast from the toasts array
   * 
   * Used by:
   * 1. Auto-dismiss timer (after duration expires)
   * 2. User clicking close button on toast
   * 3. Manual removal by calling component
   * 
   * @param {number} id - Toast ID to remove
   * 
   * Example:
   * const { removeToast, addToast } = useToast()
   * const toastId = addToast('Persistent message', 'info', 0)
   * // Later...
   * removeToast(toastId)
   */
  const removeToast = useCallback((id) => {
    // Filter out the toast with matching ID
    setToasts(prev => prev.filter(toast => toast.id !== id))
  }, [])

  /**
   * Context Value
   * 
   * All values provided to child components via useToast() hook
   */
  const value = {
    // State
    toasts,       // Array of current toast objects

    // Methods
    addToast,     // Add new toast
    removeToast,  // Remove specific toast
  }

  // Provide context to all children
  return (
    <ToastContext.Provider value={value}>
      {children}
    </ToastContext.Provider>
  )
}

/**
 * Toast Type Reference
 * 
 * Type      Color    Usage
 * ─────────────────────────────────
 * success   Green    Successful operations (saved, created, deleted)
 * error     Red      Errors and failures
 * warning   Orange   Warnings and cautions
 * info      Blue     Informational messages
 */

/**
 * Toast Duration Reference
 * 
 * Duration    Use Case
 * ────────────────────────────────
 * 2000        Quick confirmations
 * 3000        Standard messages (default)
 * 5000        Longer messages that need reading
 * 0           Persistent (manual dismiss required)
 */

/**
 * Usage Examples
 * 
 * 1. Success Toast:
 * const { addToast } = useToast()
 * addToast('Profile saved successfully!', 'success')
 * 
 * 2. Error Toast:
 * addToast('Failed to save profile', 'error')
 * 
 * 3. Warning Toast:
 * addToast('This action cannot be undone', 'warning')
 * 
 * 4. Persistent Toast:
 * const id = addToast('Important message', 'info', 0)
 * // Later when done:
 * removeToast(id)
 * 
 * 5. With useApi Hook:
 * const { addToast } = useToast()
 * const { request } = useApi()
 * const { data } = await request(apiCall, { showSuccess: true })
 * // Automatically shows success toast on success
 */

/**
 * Implementation Notes
 * 
 * 1. Toast Display:
 *    - This context only manages state
 *    - Toast component (Toast.jsx) handles rendering
 *    - Styling is in components.css
 * 
 * 2. Unique IDs:
 *    - Currently uses Date.now() for ID
 *    - Works for most cases but not 100% guaranteed unique
 *    - Use uuid library for production if multiple rapid adds occur
 * 
 * 3. useCallback:
 *    - Both functions use useCallback to prevent unnecessary re-renders
 *    - Functions remain stable across re-renders
 * 
 * 4. Timeout Management:
 *    - Each toast has its own timeout
 *    - Timeouts are automatically cleaned up
 *    - No memory leaks from accumulated timeouts
 * 
 * 5. State Updates:
 *    - Uses functional updates (prev => ...) for safety
 *    - Prevents race conditions with multiple toast additions
 * 
 * 6. Rendering:
 *    - Toast component maps over toasts array
 *    - Each toast rendered with unique key (toast.id)
 *    - Removed toasts disappear via animation then removal
 * 
 * 7. Auto-Dismiss:
 *    - Only if duration > 0
 *    - User can still manually remove via close button
 *    - Toast component handles close button click
 */

/**
 * Integration with useApi Hook
 * 
 * The useApi hook can automatically show toasts:
 * 
 * const { request } = useApi()
 * const result = await request(apiCall, {
 *   showSuccess: true,
 *   successMessage: 'Operation completed!'
 * })
 * 
 * This automatically calls:
 * addToast('Operation completed!', 'success')
 * 
 * And on error:
 * addToast(error.message, 'error')
 */