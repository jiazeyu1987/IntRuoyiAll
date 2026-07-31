import request from '@/config/axios'

export interface ProcessPoolFifoOrchestrationAllocateReqVO {
  allocationBatchNo: string
  sourceProcessId: number
  targetRouteProcessId: number
  targetProcessId: number
  targetWorkOrderIds: number[]
}

export interface ProcessPoolFifoOrchestrationLineVO {
  sourceQuantityFragmentId: number
  sourceEventId: number
  targetWorkOrderId: number
  targetWorkOrderCode: string
  allocatedQuantity: number | string
  allocationStatus: string
}

export interface ProcessPoolFifoOrchestrationAllocateRespVO {
  totalAllocatedQuantity: number | string
  lines: ProcessPoolFifoOrchestrationLineVO[]
}

export const allocateAvailableProcessPoolOutput = async (
  data: ProcessPoolFifoOrchestrationAllocateReqVO
) => {
  return await request.post<ProcessPoolFifoOrchestrationAllocateRespVO>({
    url: '/mes/pro/process-pool/fifo-orchestration/allocate-available-output',
    data
  })
}
