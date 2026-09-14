import request from '@/config/axios'

export type BackupPlanWeekday = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN'
export type BackupKind = 'FULL' | 'INCREMENTAL'
export type BackupPlanHealthStatus = '正常' | '已关闭' | '上次失败' | '配置异常'

export interface BackupPlanScheduleReqVO {
  fullSchedule: string
  incrementalSchedule: string
}

export interface BackupPlanBackupPointVO {
  backupId: string
  backupKind?: 'FULL' | 'INCREMENTAL'
  baseBackupId?: string
  parentBackupId?: string
  manifestPath?: string
  checksumPath?: string
  rehearsalReportPath?: string
  snapshotPath?: string
  lastVerifiedAt?: string
  completedAt?: string
  imageTag?: string
  backupMode?: string
  retentionKeepLast?: number
  retentionKeepDays?: number
  retentionMaxNasUsedPercent?: number
  objectAddedCount?: number
  objectModifiedCount?: number
  objectDeletedCount?: number
  objectReusedCount?: number
  recoverabilityStatus?: 'RECOVERABLE' | 'UNRECOVERABLE' | string
  dccBackupMode?: string
  dccChainStatus?: string
  dccChangeSummary?: Record<string, string>
  rehearsalStatus?: string
  unrecoverableReasons?: string[]
}

export interface BackupPlanStatusVO {
  planStatus: '已开启' | '已关闭' | string
  healthStatus: BackupPlanHealthStatus | string
  fullSchedule: string
  incrementalSchedule: string
  repositoryEnvironment?: 'test' | 'backup' | string
  maxFreshnessHours?: number
  nextRunTime?: string
  lastRunTime?: string
  lastResultCode?: number
  blockedReason?: string
  latestBackupPoint?: BackupPlanBackupPointVO
}

export interface BackupPlanOperationVO {
  operationId: string
  status: string
  summary?: string
}

export const getBackupPlanStatus = () => {
  return request.get<BackupPlanStatusVO>({ url: '/infra/backup-plan/status' })
}

export const saveBackupPlanSchedule = (data: BackupPlanScheduleReqVO) => {
  return request.put<BackupPlanStatusVO>({ url: '/infra/backup-plan/schedule', data })
}

export const enableBackupPlan = () => {
  return request.post<BackupPlanStatusVO>({ url: '/infra/backup-plan/enable' })
}

export const disableBackupPlan = () => {
  return request.post<BackupPlanStatusVO>({ url: '/infra/backup-plan/disable' })
}

export const backupNow = (backupKind: BackupKind) => {
  return request.post<BackupPlanOperationVO>({
    url: '/infra/backup-plan/backup-now',
    params: { backupKind }
  })
}

export const downloadBackupEvidence = () => {
  return request.download<Blob>({
    url: '/infra/backup-plan/evidence/export',
    responseType: 'blob'
  })
}

export const getBackupPlanHistoryPage = (params: PageParam) => {
  return request.get<PageResult<BackupPlanBackupPointVO[]>>({
    url: '/infra/backup-plan/history/page',
    params
  })
}
