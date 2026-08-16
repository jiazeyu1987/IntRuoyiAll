import request from '@/config/axios'

export type BackupPlanFrequency = 'DAILY' | 'WEEKLY'
export type BackupPlanWeekday = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN'
export type BackupPlanHealthStatus = '正常' | '已关闭' | '上次失败' | '配置异常'

export interface BackupPlanScheduleReqVO {
  frequency: BackupPlanFrequency
  time: string
  weekday?: BackupPlanWeekday
}

export interface BackupPlanBackupPointVO {
  backupId: string
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
  frequency: BackupPlanFrequency
  time: string
  weekday?: BackupPlanWeekday
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

export const backupNow = () => {
  return request.post<BackupPlanOperationVO>({ url: '/infra/backup-plan/backup-now' })
}

export const getBackupPlanHistoryPage = (params: PageParam) => {
  return request.get<PageResult<BackupPlanBackupPointVO[]>>({
    url: '/infra/backup-plan/history/page',
    params
  })
}
