import request from '@/config/axios'

export interface InternUserFileUploadTimeUpdateReqVO {
  id: number
  createTime: string
}

export interface InternUserDccTimeUpdateReqVO {
  controlledFileId: string
  targetTime: string
}

export interface InternUserTimeMaintenanceAuditVO {
  id: number
  targetType: string
  targetId: string
  targetName?: string
  fieldName: string
  oldTime?: string
  newTime: string
  operatorUserId?: number
  createTime: string
}

// 实习用户模块：修改文件上传时间
export const updateFileUploadTime = (data: InternUserFileUploadTimeUpdateReqVO) => {
  return request.put({ url: '/intern-user/time-maintenance/file/upload-time', data })
}

// 实习用户模块：查看文件上传时间修改审计
export const getFileUploadTimeAudits = (fileId: number | string) => {
  return request.get<InternUserTimeMaintenanceAuditVO[]>({
    url: '/intern-user/time-maintenance/file/upload-time/audits',
    params: { fileId }
  })
}

// 实习用户模块：修改 DCC 升版时间
export const updateDccPublishedTime = (data: InternUserDccTimeUpdateReqVO) => {
  return request.put({ url: '/intern-user/time-maintenance/dcc/published-time', data })
}

// 实习用户模块：查看 DCC 升版时间修改审计
export const getDccPublishedTimeAudits = (controlledFileId: string) => {
  return request.get<InternUserTimeMaintenanceAuditVO[]>({
    url: '/intern-user/time-maintenance/dcc/published-time/audits',
    params: { controlledFileId }
  })
}

// 实习用户模块：修改 DCC 作废时间
export const updateDccObsoletedTime = (data: InternUserDccTimeUpdateReqVO) => {
  return request.put({ url: '/intern-user/time-maintenance/dcc/obsoleted-time', data })
}

// 实习用户模块：查看 DCC 作废时间修改审计
export const getDccObsoletedTimeAudits = (controlledFileId: string) => {
  return request.get<InternUserTimeMaintenanceAuditVO[]>({
    url: '/intern-user/time-maintenance/dcc/obsoleted-time/audits',
    params: { controlledFileId }
  })
}
