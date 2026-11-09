import request from '@/config/axios'
import type { DccRegistrationCertificatePageItemVO } from './index'
const base = '/dcc/registration-certificates/handling-tasks'
export interface HandlingConfig { responsibleUserId: string; responsibleName: string; projectUploadDays: number; renewalDays: number }
export interface HandlingTask {
 id: string; taskKind: 'UPLOAD'|'RENEWAL'; projectCodeId?: string; certificateId?: string; title: string
 responsibleUserId: string; responsibleName: string; assignedUserId: string; assignedName: string
 dueAt: number; status: 'OPEN'|'SUBMITTED'|'COMPLETED'; actualHandlerId?: string; lastRequestId?: string; actualHandlerName?: string; result?: string; completedAt?: string
}
export interface TaskProject { id: string; projectCode: string; projectName: string }
export interface HandlingDetail {
 task: HandlingTask; events: Array<{ eventType: string; actorId: string; actorName: string; assignedName?: string; assignedUserId?: string; dueAt?: string; requestId?: string; reason: string; createdAt: string }>
 canDelegate: boolean; canHandle: boolean; restartRequestId?: string; project?: TaskProject; certificate?: DccRegistrationCertificatePageItemVO
}
type HandlingCertificateWire = Omit<DccRegistrationCertificatePageItemVO, 'firstObtainedDate'|'effectiveDate'|'expiryDate'> & {
 firstObtainedDate?: number|null; effectiveDate: number|null; expiryDate: number|null
}
type HandlingDetailWire = Omit<HandlingDetail, 'certificate'> & { certificate?: HandlingCertificateWire }
const certificateDateFormatter = new Intl.DateTimeFormat('en-CA', {
 timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit'
})
const certificateDate = (value: number|null|undefined, field: string): string => {
 if (typeof value !== 'number' || !Number.isFinite(value) || !Number.isFinite(new Date(value).getTime())) {
  throw new Error(`办理任务注册证${field}缺失或不是有效的毫秒时间戳`)
 }
 const parts = certificateDateFormatter.formatToParts(value)
 return ['year', 'month', 'day'].map(type => parts.find(part => part.type === type)!.value).join('-')
}
export const getHandlingConfig = () => request.get<HandlingConfig|null>({ url: `${base}/config` })
export const saveHandlingConfig = (data: Omit<HandlingConfig,'responsibleName'>) => request.put<HandlingConfig>({ url: `${base}/config`, data })
export const getHandlingCandidates = (keyword: string, taskId?: string) => request.get<Array<{id:string;name:string}>>({url: `${base}/candidates`, params:{keyword,taskId}})
export const getHandlingTask = async (id: string): Promise<HandlingDetail> => {
 const detail = await request.get<HandlingDetailWire>({url:`${base}/${id}`})
 if (!detail.certificate) return { ...detail, certificate: undefined }
 const certificate = detail.certificate
 return { ...detail, certificate: {
  ...certificate,
  firstObtainedDate: certificate.firstObtainedDate == null ? undefined : certificateDate(certificate.firstObtainedDate, '首次获证日'),
  effectiveDate: certificateDate(certificate.effectiveDate, '生效日期'),
  expiryDate: certificateDate(certificate.expiryDate, '有效期至')
 } }
}
export const delegateHandlingTask = (id: string, data: {assignedUserId:string; dueAt:number; reason:string}) => request.post({url:`${base}/${id}/delegate`,data})
