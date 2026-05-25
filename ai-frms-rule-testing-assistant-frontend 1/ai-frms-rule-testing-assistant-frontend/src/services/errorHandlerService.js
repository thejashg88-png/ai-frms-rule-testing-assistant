export const errorHandlerService = {
  // Get error message from various error types
  getErrorMessage: (error) => {
    if (typeof error === 'string') {
      return error
    }

    if (error.response?.data?.message) {
      return error.response.data.message
    }

    if (error.response?.data?.error) {
      return error.response.data.error
    }

    if (error.message) {
      return error.message
    }

    return 'An unexpected error occurred'
  },

  // Get error status code
  getStatusCode: (error) => {
    return error.response?.status || null
  },

  // Handle specific error codes
  handleErrorCode: (statusCode) => {
    const errorMessages = {
      400: 'Bad request. Please check your input.',
      401: 'Unauthorized. Please login again.',
      403: 'Forbidden. You do not have permission to access this resource.',
      404: 'Not found. The requested resource does not exist.',
      409: 'Conflict. This resource already exists.',
      422: 'Validation error. Please check your input.',
      500: 'Internal server error. Please try again later.',
      503: 'Service unavailable. Please try again later.',
    }

    return errorMessages[statusCode] || 'An error occurred. Please try again.'
  },

  // Check if error is network error
  isNetworkError: (error) => {
    return !error.response
  },

  // Check if error is timeout
  isTimeout: (error) => {
    return error.code === 'ECONNABORTED'
  },
}

export default errorHandlerService