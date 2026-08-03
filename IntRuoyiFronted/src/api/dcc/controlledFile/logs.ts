import request from '@/config/axios'

export type DccControlledFileLogType =
  | 'CONTROLLED_FILE_AUDIT'
  | 'FILE_SUBMISSION'
  | 'FILE_APPROVAL'
  | 'FILE_RELEASE'
  | 'FILE_DISTRIBUTION'
  | 'FILE_REVISION'
  | 'FILE_OBSOLETE'
  | 'PROJECT_CODE_ASSIGNMENT'
  | 'PROJECT_CODE_CHANGE'
  | 'TRAINING_EXECUTION'

export interface DccControlledFileLogPageReqVO extends PageParam {
  logType?: DccControlledFileLogType | string
  keyword?: string
  actionType?: string
  result?: string
  controlledFileId?: number
  projectCodeId?: number
  assignmentId?: number
  operatorUserId?: number
  fieldName?: string
  occurredAt?: string[]
}

export interface DccControlledFileLogRespVO {
  id: string
  logType: DccControlledFileLogType | string
  sourceRecordId?: number | null
  occurredAt?: string | null
  actionLabel?: string | null
  resultLabel?: string | null
  fileNumber?: string | null
  fileName?: string | null
  versionNo?: string | null
  operatorUserId?: number | null
  operatorName?: string | null
  relatedObject?: string | null
  summary?: string | null
  oldValueText?: string | null
  newValueText?: string | null
  reason?: string | null
  detailJson?: string | null
}

export const getControlledFileLogPage = async (
  params: DccControlledFileLogPageReqVO
): Promise<PageResult<DccControlledFileLogRespVO[]>> => {
  return await request.get({
    url: '/dcc/controlled-file-logs/page',
    params,
    ignoreErrorMessage: true
  })
}
