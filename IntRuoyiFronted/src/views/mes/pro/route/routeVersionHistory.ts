import type { ProRouteVersionVO } from '@/api/mes/pro/route'

export const LEGACY_ROUTE_VERSION_STATUS = 'LEGACY_INCOMPLETE'
export const LEGACY_ROUTE_SUMMARY_FORMAT = 'MES_ROUTE_LEGACY_SUMMARY_V1'
export const LEGACY_ROUTE_VERSION_NOTICE =
  '历史摘要不完整，不可执行。原始摘要和受控引用予以保留，不能用于新排程、审批或执行。新业务请正常配置并发布完整新版本。'

export const isLegacyRouteVersion = (
  version: Pick<ProRouteVersionVO, 'lifecycleStatus' | 'routeSnapshotFormatVersion'>
) =>
  version.lifecycleStatus === LEGACY_ROUTE_VERSION_STATUS ||
  version.routeSnapshotFormatVersion === LEGACY_ROUTE_SUMMARY_FORMAT

export const requireLegacyRouteSummary = (version: ProRouteVersionVO) => {
  if (!isLegacyRouteVersion(version) || !version.routeSnapshotJson?.trim()) {
    throw new Error('历史摘要加载失败：未返回真实遗留摘要，无法展示历史详情。')
  }
  return version.routeSnapshotJson
}

export const readLegacyRouteSummaryFields = (version: ProRouteVersionVO) => {
  const summary: unknown = JSON.parse(requireLegacyRouteSummary(version))
  if (!summary || typeof summary !== 'object' || Array.isArray(summary)) {
    throw new Error('历史摘要加载失败：原始摘要不是 JSON 对象。')
  }
  return summary as Record<string, unknown>
}

export const formatLegacyRouteSummaryField = (summary: Record<string, unknown>, field: string) => {
  if (!Object.prototype.hasOwnProperty.call(summary, field)) return '原摘要未保存此字段'
  const value = summary[field]
  if (value === null) return '原值为 null'
  if (value === '') return '原值为空字符串'
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}
