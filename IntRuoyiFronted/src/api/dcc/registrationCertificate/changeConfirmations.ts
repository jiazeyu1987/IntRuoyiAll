import request from '@/config/axios'

export interface ChangeConfirmation {
  id: number | string
  certificateId: number | string
  versionId: number | string
  eventKey: string
  recipientUserId: number | string
  notifyMessageId: number | string
  sentAt: number
  confirmedAt?: number
}
const base = '/dcc/registration-certificates/change-confirmations'
export const listChangeConfirmations = (certificateId: number | string) =>
  request.get<ChangeConfirmation[]>({ url: `${base}/certificate/${certificateId}` })
export const getMyChangeConfirmation = (messageId: number | string) =>
  request.get<ChangeConfirmation>({ url: `${base}/message/${messageId}` })
export const confirmMyCertificateChange = (messageId: number | string) =>
  request.post<ChangeConfirmation>({ url: `${base}/message/${messageId}/confirm` })
