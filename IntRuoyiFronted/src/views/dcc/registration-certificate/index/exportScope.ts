import type { DccRegistrationCertificatePageReqVO } from '@/api/dcc/registrationCertificate'

const canonical = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(canonical)
  if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .sort(([a], [b]) => a.localeCompare(b)).map(([k, v]) => [k, canonical(v)])
    .filter(([, v]) => JSON.stringify(v) !== '{}'))
  return value
}
export const exportQueryKey = (query: DccRegistrationCertificatePageReqVO, includeSort = false) => {
  const { pageNo, pageSize, sortField, sortOrder, ...filters } = query
  return JSON.stringify(canonical(includeSort ? { ...filters, sortField, sortOrder } : filters))
}
export const cloneExportQuery = (query: DccRegistrationCertificatePageReqVO): DccRegistrationCertificatePageReqVO => JSON.parse(JSON.stringify(query))
