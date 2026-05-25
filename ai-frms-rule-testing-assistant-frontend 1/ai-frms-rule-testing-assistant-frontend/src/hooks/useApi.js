import { useState, useCallback } from 'react'
import { useToast } from './useToast'

export const useApi = () => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const { addToast } = useToast()

  const request = useCallback(async (apiFunction, options = {}) => {
    const { showSuccess = true, successMessage = 'Success!' } = options

    try {
      setLoading(true)
      setError(null)
      const response = await apiFunction()
      
      if (showSuccess) {
        addToast(successMessage, 'success')
      }

      return { data: response, error: null }
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || 'An error occurred'
      setError(errorMessage)
      addToast(errorMessage, 'error')
      return { data: null, error: errorMessage }
    } finally {
      setLoading(false)
    }
  }, [addToast])

  return { loading, error, request }
}

export default useApi