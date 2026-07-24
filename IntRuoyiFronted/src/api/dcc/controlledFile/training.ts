import axios from 'axios'
import request from '@/config/axios'
import { config as axiosConfig } from '@/config/axios/config'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import type { ControlledPreviewWatermark } from './workflow'
import { CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER } from './workflow'

export interface TrainingTaskProgressVO {
  progressId: number
  controlledFileId: number
  categoryId: number
  fileName?: string
  title: string
  fileNumber?: string
  versionNo: string
  fileStatus: string
  userId: number
  departmentIds: number[]
  requiredViewSeconds: number
  accumulatedViewSeconds: number
  eligibleToAcknowledge: boolean
  firstViewedAt?: string
  lastViewedAt?: string
  acknowledgedAt?: string
  publishedTime?: string
  status: 'PENDING_VIEW' | 'READY_TO_ACKNOWLEDGE' | 'ACKNOWLEDGED'
}

export interface TrainingExecutionRowVO {
  progressId: number
  controlledFileId: number
  categoryId: number
  fileName?: string
  title: string
  fileNumber?: string
  versionNo: string
  fileStatus: string
  userId: number
  departmentIds: number[]
  requiredViewSeconds: number
  accumulatedViewSeconds: number
  eligibleToAcknowledge: boolean
  firstViewedAt?: string
  lastViewedAt?: string
  acknowledgedAt?: string
  publishedTime?: string
  status: 'PENDING_VIEW' | 'READY_TO_ACKNOWLEDGE' | 'ACKNOWLEDGED'
}

export interface TrainingTaskPageReqVO extends PageParam {
  categoryId?: number
  status?: string
}

export interface TrainingExecutionPageReqVO extends PageParam {
  categoryId?: number
  status?: string
}

export interface TrainingViewSessionReqVO {
  clientSessionId: string
}

export interface TrainingTaskPreviewWithWatermark {
  blob: Blob
  watermark: ControlledPreviewWatermark | null
}

const buildBinaryHeaders = () => {
  const headers: Record<string, string> = {}
  const accessToken = getAccessToken()
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
  const tenantId = getTenantId()
  if (tenantId) {
    headers['tenant-id'] = tenantId
  }
  const visitTenantId = getVisitTenantId()
  if (visitTenantId && accessToken) {
    headers['visit-tenant-id'] = visitTenantId
  }
  headers['Cache-Control'] = 'no-cache'
  headers.Pragma = 'no-cache'
  return headers
}

const decodePreviewWatermark = (rawHeader: unknown): ControlledPreviewWatermark | null => {
  const encoded = typeof rawHeader === 'string' ? rawHeader.trim() : ''
  if (!encoded) {
    return null
  }
  try {
    const normalized = encoded.replace(/-/g, '+').replace(/_/g, '/')
    const paddingLength = (4 - (normalized.length % 4 || 4)) % 4
    const padded = normalized.padEnd(normalized.length + paddingLength, '=')
    const decoded = atob(padded)
    const bytes = Uint8Array.from(decoded, (char) => char.charCodeAt(0))
    const text = new TextDecoder().decode(bytes)
    const payload = JSON.parse(text)
    if (!payload || typeof payload !== 'object') {
      return null
    }
    return payload as ControlledPreviewWatermark
  } catch {
    return null
  }
}

export const getMyTrainingTaskPage = async (
  params: TrainingTaskPageReqVO
): Promise<PageResult<TrainingTaskProgressVO[]>> => {
  return await request.get({ url: '/dcc/training-tasks/my-page', params })
}

export const getTrainingTask = async (progressId: number | string): Promise<TrainingTaskProgressVO> => {
  return await request.get({ url: `/dcc/training-tasks/${progressId}` })
}

export const previewTrainingTaskWithWatermark = async (
  progressId: number | string
): Promise<TrainingTaskPreviewWithWatermark> => {
  const response = await axios.get<Blob>(
    `${axiosConfig.base_url}/dcc/training-tasks/${progressId}/preview`,
    {
      headers: buildBinaryHeaders(),
      timeout: axiosConfig.request_timeout,
      responseType: 'blob'
    }
  )
  return {
    blob: response.data,
    watermark: decodePreviewWatermark(
      response.headers?.[CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER] ||
        response.headers?.[CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER.toLowerCase()]
    )
  }
}

export const startTrainingViewSession = async (
  progressId: number | string,
  data: TrainingViewSessionReqVO
): Promise<TrainingTaskProgressVO> => {
  return await request.post({ url: `/dcc/training-tasks/${progressId}/view-session/start`, data })
}

export const heartbeatTrainingViewSession = async (
  progressId: number | string,
  data: TrainingViewSessionReqVO
): Promise<TrainingTaskProgressVO> => {
  return await request.post({ url: `/dcc/training-tasks/${progressId}/view-session/heartbeat`, data })
}

export const stopTrainingViewSession = async (
  progressId: number | string,
  data: TrainingViewSessionReqVO
): Promise<TrainingTaskProgressVO> => {
  return await request.post({ url: `/dcc/training-tasks/${progressId}/view-session/stop`, data })
}

export const stopTrainingViewSessionKeepalive = async (
  progressId: number | string,
  data: TrainingViewSessionReqVO
): Promise<void> => {
  await fetch(`${axiosConfig.base_url}/dcc/training-tasks/${progressId}/view-session/stop`, {
    method: 'POST',
    headers: {
      ...buildBinaryHeaders(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data),
    keepalive: true
  })
}

export const acknowledgeTrainingTask = async (progressId: number | string): Promise<boolean> => {
  return await request.post({ url: `/dcc/training-tasks/${progressId}/acknowledge` })
}

export const getTrainingExecutionPage = async (
  params: TrainingExecutionPageReqVO
): Promise<PageResult<TrainingExecutionRowVO[]>> => {
  return await request.get({ url: '/dcc/training-executions/page', params })
}
