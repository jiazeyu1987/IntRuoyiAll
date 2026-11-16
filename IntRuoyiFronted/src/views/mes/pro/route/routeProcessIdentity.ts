import type { MesRouteId } from '@/api/mes/pro/route'

// IDs are identities, never quantities. Reject numbers whose original digits are already lost.
export const requireRouteProcessId = (value: unknown): MesRouteId => {
  if (typeof value === 'number') {
    if (!Number.isSafeInteger(value) || value === 0)
      throw new Error(`路线工序编号必须保持完整整数精度：${value}`)
    return value
  }
  if (typeof value === 'string' && /^-?[1-9]\d*$/.test(value)) return value
  throw new Error(`路线工序编号无效：${String(value)}`)
}

export const routeProcessIdKey = (value: unknown): string => String(requireRouteProcessId(value))

export const sameRouteProcessId = (left: unknown, right: unknown): boolean => {
  if (left == null || right == null || left === '' || right === '') return false
  return routeProcessIdKey(left) === routeProcessIdKey(right)
}
