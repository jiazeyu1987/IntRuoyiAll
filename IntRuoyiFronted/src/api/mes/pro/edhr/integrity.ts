import request from '@/config/axios'

export interface IntegrityError {
  batchId: number
  batchExecutionCode: string
  message: string
}
export interface IntegrityChunk {
  upperId: number
  nextId: number
  checked: number
  status: 'RUNNING' | 'COMPLETED'
  errors: IntegrityError[]
}
export const checkIntegrityChunk = (data: { afterId?: number; upperId?: number }) =>
  request.post<IntegrityChunk>({ url: '/mes/pro/edhr-batch-execution/integrity/check-chunk', data })
