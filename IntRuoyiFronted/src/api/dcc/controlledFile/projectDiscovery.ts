import request from '@/config/axios'
import type { DccProjectCodePageReqVO } from './projectCodes'

/** Public project read projection: stable identities, never inferred from a displayed leader name. */
export interface DccDiscoveredProject {
  id: string
  projectName: string
  projectCode: string
  status: string
  projectLeaderUserId: string | null
  projectLeader: string | null
  associatedFileCount: number | null
}

const identity = (value: unknown): string => {
  if (typeof value !== 'string' && typeof value !== 'number') throw new Error('项目身份缺失')
  if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error('项目身份已超出安全整数精度')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id)) throw new Error('项目身份不合法')
  return id
}

const readProject = (value: unknown, allowIncomplete = false): DccDiscoveredProject => {
  if (!value || typeof value !== 'object') throw new Error('项目详情响应缺失')
  const row = value as Record<string, unknown>
  if ((!allowIncomplete && (typeof row.projectName !== 'string' || !row.projectName.trim() || typeof row.projectCode !== 'string'))
    || (row.projectName != null && typeof row.projectName !== 'string')
    || (row.projectCode != null && typeof row.projectCode !== 'string') || typeof row.status !== 'string'
    || (row.projectLeader != null && typeof row.projectLeader !== 'string')
    || (row.associatedFileCount != null && (!Number.isSafeInteger(row.associatedFileCount) || Number(row.associatedFileCount) < 0))) {
    throw new Error('项目读取响应缺少正式字段')
  }
  return {
    id: identity(row.id), projectName: row.projectName == null ? '' : row.projectName as string,
    projectCode: row.projectCode == null ? '' : row.projectCode as string, status: row.status,
    projectLeaderUserId: row.projectLeaderUserId == null ? null : identity(row.projectLeaderUserId),
    projectLeader: row.projectLeader == null ? null : row.projectLeader as string,
    associatedFileCount: row.associatedFileCount == null ? null : Number(row.associatedFileCount)
  }
}

export const getProjectDiscoveryPage = async (params: DccProjectCodePageReqVO): Promise<PageResult<DccDiscoveredProject[]>> => {
  const response = await request.get<{ list: unknown[]; total: number }>({ url: '/dcc/project-codes/page', params, ignoreErrorMessage: true })
  if (!response || !Array.isArray(response.list) || !Number.isSafeInteger(response.total) || response.total < 0) {
    throw new Error('项目分页响应缺失或total不合法')
  }
  // Keep the server-authorized total/order/page; validation must not filter rows or invent pagination.
  return { list: response.list.map(value => readProject(value)), total: response.total }
}

export interface DccUploadProjectOption extends DccDiscoveredProject {
  uploadSelectable: boolean
  uploadIssues: string[]
}

export const getProjectUploadOptions = async (keyword = ''): Promise<DccUploadProjectOption[]> => {
  const response = await request.get<unknown[]>({ url: '/dcc/project-codes/upload-options',
    params: { keyword: keyword.trim() || undefined }, ignoreErrorMessage: true })
  if (!Array.isArray(response)) throw new Error('上传项目候选响应缺失')
  return response.map(value => {
    const project = readProject(value, true)
    const row = value as Record<string, unknown>
    if (typeof row.uploadSelectable !== 'boolean' || !Array.isArray(row.uploadIssues)
      || row.uploadIssues.some(issue => typeof issue !== 'string' || !issue.trim())
      || row.uploadSelectable !== (row.uploadIssues.length === 0)
      || (row.uploadSelectable && (!project.projectName.trim() || !project.projectCode.trim()))) throw new Error('项目可选状态或阻断原因不合法')
    return { ...project, uploadSelectable: row.uploadSelectable, uploadIssues: row.uploadIssues as string[] }
  })
}

export const getProjectDiscovery = async (projectId: string | number): Promise<DccDiscoveredProject> => {
  const id = identity(projectId)
  const row = readProject(await request.get({ url: `/dcc/project-codes/${id}`, ignoreErrorMessage: true }))
  if (row.id !== id) throw new Error('项目详情身份与选择不一致')
  return row
}
