import request from '@/config/axios'
import type { DccRegistrationCertificatePageReqVO } from './index'

export interface ExportPreview {
  snapshotId: string
  requestId: string
  mode: 'SELECTED' | 'FILTERED'
  count: number
  types: Record<string, number>
  maxRecords: number
  allowed: boolean
  reason: string
  snapshotExpiresAt: string | number
  structureNotices: string[]
}
export interface ExportStatus {
  state: 'CONFIRMED' | 'GENERATING' | 'READY' | 'FAILED' | 'EXPIRED' | 'INVALID'
  reason: string
  exported: number
  skipped: number
  fileName: string
  expiresAt: string | number
  structureNotices: string[]
}
const base = '/dcc/registration-certificates/excel-export'
export const previewExcelExport = (mode: ExportPreview['mode'], query: DccRegistrationCertificatePageReqVO, recordIds: (string | number)[]) =>
  request.post<ExportPreview>({ url: `${base}/snapshot`, data: { tabType: 'current', mode, query, recordIds } })
export const getExcelExportStatus = (requestId: string) => request.get<ExportStatus>({ url: `${base}/${requestId}/status` })
export const fetchExcelExport = async (preview: Pick<ExportPreview, 'requestId' | 'snapshotId'>, retry: boolean) => {
  const response = await request.downloadOriginal<any>({
    url: retry ? `${base}/${preview.requestId}/file` : `${base}/generate`,
    method: retry ? 'GET' : 'POST',
    data: retry ? undefined : { snapshotId: preview.snapshotId, requestId: preview.requestId }
  })
  const disposition = response.headers['content-disposition']
  const name = /filename\*=UTF-8''([^;]+)/i.exec(disposition || '')?.[1]
  if (!name) throw new Error('Excel响应缺少有效文件名')
  const exported = Number(response.headers['x-export-count'])
  const skipped = Number(response.headers['x-export-skipped'])
  if (!Number.isInteger(exported) || exported <= 0 || !Number.isInteger(skipped) || skipped < 0)
    throw new Error('Excel响应缺少有效导出计数')
  return { blob: response.data as Blob, fileName: decodeURIComponent(name), exported, skipped }
}
