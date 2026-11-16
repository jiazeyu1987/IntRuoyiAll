import request from '@/config/axios'

const base = '/mes/pro/process-pool/team-leader/active-order/dossier-files'
export type DossierId = number | string
export interface DossierResponsibility {
  categoryKey: string
  workTaskId: DossierId
  assigneeUserId: DossierId
  assigneeUserName: string
  status: string
  reason: string
}
export interface DossierCompletionTask {
  workTaskId: DossierId
  activeOrderId: number
  applicationId?: DossierId
  categoryKey: string
  categoryLabel: string
  workOrderCode: string
  reason: string
  status: string
  writable: boolean
}
export const getDossierResponsibilities = (activeOrderId: DossierId) =>
  request.get<DossierResponsibility[]>({ url: `${base}/responsibilities`, params: { activeOrderId } })
export const assignDossierResponsibility = (data: { activeOrderId: DossierId; categoryKey: string; assigneeUserId: DossierId; reason: string }) =>
  request.post<DossierId>({ url: `${base}/assign`, data })
export const getDossierCompletionTask = (workTaskId: DossierId) =>
  request.get<DossierCompletionTask>({ url: `${base}/completion-task`, params: { workTaskId } })
