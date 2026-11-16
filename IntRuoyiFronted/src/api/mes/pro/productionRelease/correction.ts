import request from '@/config/axios'
import type {
  ActiveOrderDossierFilesRespVO,
  ActiveOrderDossierFileItemVO
} from '@/api/mes/pro/processpool/teamLeader'

const base = '/mes/pro/production-release/pqc/correction'
export const receivePqcReleaseCorrection = (applicationId: string) =>
  request.post<string>({ url: `${base}/receive`, data: { applicationId }, ignoreErrorMessage: true })
export interface PqcReleaseCorrectionTask {
  workTaskId: string
  applicationId: string
  activeOrderId: string
  batchExecutionId: string
  workOrderCode: string
  batchCode: string
  assigneeUserId: string
  rejectReason: string
  taskStatus: string
  expectedVersion: number
  rejectedDossierRevision: number
  currentDossierRevision: number
  writable: boolean
  nextApplicationId?: string
}
export interface PqcReleaseCorrectionReceipt {
  workTaskId: string
  applicationId: string
  pqcReleaseWorkTaskId: string
  status: string
}
export const getPqcReleaseCorrection = (workTaskId: string) =>
  request.get<PqcReleaseCorrectionTask>({
    url: `${base}/get`,
    params: { workTaskId },
    ignoreErrorMessage: true
  })
export const getPqcReleaseCorrectionFiles = (workTaskId: string) =>
  request.get<ActiveOrderDossierFilesRespVO>({
    url: `${base}/files`,
    params: { workTaskId },
    ignoreErrorMessage: true
  })
export const resubmitPqcReleaseCorrection = (data: {
  workTaskId: string
  expectedVersion: number
  idempotencyKey: string
  correctionNote: string
}) =>
  request.post<PqcReleaseCorrectionReceipt>({
    url: `${base}/resubmit`,
    data,
    ignoreErrorMessage: true
  })

export const uploadPqcReleaseCorrectionFile = async (
  task: PqcReleaseCorrectionTask,
  categoryKey: string,
  file: File
) => {
  const data = new FormData()
  data.append('activeOrderId', task.activeOrderId)
  data.append('applicationId', task.applicationId)
  data.append('categoryKey', categoryKey)
  data.append('file', file)
  const response = await request.upload<{ data: ActiveOrderDossierFileItemVO }>({
    url: '/mes/pro/process-pool/team-leader/active-order/dossier-files/upload',
    data,
    timeout: 180000
  })
  if (!response.data) throw new Error('补正资料上传未取得正式文件回执，请刷新核对')
  return response.data
}
