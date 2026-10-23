import request from '@/config/axios'

export interface DirectSupervisorRelation {
  tenantId: string
  userId: string
  supervisorUserId: string | null
  relationVersion: string
  username: string
  nickname: string
  supervisorUsername?: string | null
  supervisorNickname?: string | null
  supervisorStatus?: number | null
}
export interface DirectSupervisorSave {
  userId: string
  expectedRelationVersion: string
  operation: 'SET' | 'CLEAR'
  supervisorUserId: string | null
  changeReason: string
}
export interface DirectSupervisorCandidate { id: number | string; username: string; nickname: string }
export const getDirectSupervisorCandidates = (): Promise<DirectSupervisorCandidate[]> =>
  request.get({ url: '/system/user/simple-list', ignoreErrorMessage: true })
export const getDirectSupervisor = (userId: string): Promise<DirectSupervisorRelation> =>
  request.get({ url: '/system/user/direct-supervisor', params: { userId }, ignoreErrorMessage: true })
export const saveDirectSupervisor = (data: DirectSupervisorSave): Promise<DirectSupervisorRelation> =>
  request.put({ url: '/system/user/direct-supervisor', data, ignoreErrorMessage: true })
