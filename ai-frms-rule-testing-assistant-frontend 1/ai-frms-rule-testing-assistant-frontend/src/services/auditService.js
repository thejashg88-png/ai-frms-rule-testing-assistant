import auditApi from '../api/auditApi'
import errorHandlerService from './errorHandlerService'

const normalizeLog = (item) => ({
  auditId:     item.auditId     ?? item.id     ?? null,
  actor:       item.actor       ?? '-',
  action:      item.action      ?? '-',
  entityType:  item.entityType  ?? '-',
  entityId:    item.entityId    ?? null,
  entityName:  item.entityName  ?? '-',
  oldValue:    item.oldValue    ?? null,
  newValue:    item.newValue    ?? null,
  description: item.description ?? '',
  createdAt:   item.createdAt   ?? null,
})

export const auditService = {
  getAll: async ({ page = 1, size = 20, actor = '', action = '', entityType = '' } = {}) => {
    try {
      const params = { page: page - 1, size }
      if (actor)      params.actor      = actor
      if (action)     params.action     = action
      if (entityType) params.entityType = entityType

      const resp = await auditApi.getAll(params)
      // Support: ApiResponse<Page<T>>, ApiResponse<T[]>, or raw
      const raw = resp?.data?.data ?? resp?.data ?? resp

      if (raw?.content) {
        return {
          items:       raw.content.map(normalizeLog),
          totalItems:  raw.totalElements ?? raw.content.length,
          totalPages:  raw.totalPages    ?? 1,
          currentPage: page,
        }
      }
      if (Array.isArray(raw)) {
        return {
          items:       raw.map(normalizeLog),
          totalItems:  raw.length,
          totalPages:  1,
          currentPage: page,
        }
      }
      return { items: [], totalItems: 0, totalPages: 1, currentPage: page }
    } catch (err) {
      throw new Error(errorHandlerService.getErrorMessage(err))
    }
  },
}

export default auditService
