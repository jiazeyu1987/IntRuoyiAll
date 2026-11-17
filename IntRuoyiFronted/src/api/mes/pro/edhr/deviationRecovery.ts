import request from '@/config/axios'
import type { DeviationHandlingRespVO } from './deviation'

export interface DeviationRecoveryReceiver {
  id: number
  username: string
  nickname: string
}
export interface DeviationReceiverAssignment {
  deviationId: number
  expectedDeviationVersion: number
  receiverId: number | string
  reason: string
}
export interface DeviationInvestigationWithdrawal {
  deviationId: number
  expectedContentVersion: number
  expectedContentHash: string
  reason: string
}
export const getDeviationRecoveryReceivers = () => request.get<DeviationRecoveryReceiver[]>({
  url: '/mes/pro/edhr-deviation-recovery/receiver-candidates', ignoreErrorMessage: true
})
export const assignDeviationReceiver = (data: DeviationReceiverAssignment) => request.post<boolean>({
  url: '/mes/pro/edhr-deviation-recovery/assign-receiver', data, ignoreErrorMessage: true
})
export const withdrawDeviationInvestigation = (data: DeviationInvestigationWithdrawal) => request.post<DeviationHandlingRespVO>({
  url: '/mes/pro/edhr-deviation-recovery/withdraw-investigation', data, ignoreErrorMessage: true
})
