export interface BatchExecutionTraceContext {
  batchExecutionId?: string | number
  batchExecutionCode?: string
  executionId?: string | number
  executionCode?: string
  workOrderCode?: string
  batchCode?: string
  releaseTransactionId?: string | number
  sourceTab?: 'audit' | 'change' | 'reject' | 'release'
}
