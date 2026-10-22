import { reactive } from 'vue'
import { validateDistributionFacts, type DistributionRecipientFact, type AutomaticDistributionResult } from '../workflow/distribution-facts'
import type { AutomaticDistributionRetryRequest } from '@/api/dcc/controlledFile/distribution'

interface ReceiptReadContext {
  fileId: string
  processInstanceId: string
  versionNo: string
  authorityKey?: string
}
const exactResultId = (value: unknown): boolean => typeof value === 'number'
  ? Number.isSafeInteger(value) && value > 0
  : typeof value === 'string' && /^[1-9]\d*$/.test(value) && BigInt(value) <= 9223372036854775807n

export const createDistributionReceiptReader = (
  currentContext: () => ReceiptReadContext | undefined,
  read: (fileId: string) => Promise<DistributionRecipientFact[]>,
  readAutomatic: (fileId: string) => Promise<AutomaticDistributionResult>
) => {
  const state = reactive<{
    rows?: DistributionRecipientFact[]
    loading: boolean
    retrying: boolean
    error: string
    automaticResult?: AutomaticDistributionResult
    automaticResultError: string
  }>({ rows: undefined, loading: false, retrying: false, error: '', automaticResult: undefined, automaticResultError: '' })
  let generation = 0
  let activeRetry: number | undefined
  let verifiedAutomaticContext: ReceiptReadContext | undefined
  const clear = () => {
    generation++
    activeRetry = undefined
    verifiedAutomaticContext = undefined
    state.rows = undefined
    state.loading = false
    state.retrying = false
    state.error = ''
    state.automaticResult = undefined
    state.automaticResultError = ''
  }
  const load = async () => {
    const attempt = ++generation
    const selected = currentContext()
    state.rows = undefined
    state.error = ''
    state.automaticResult = undefined
    state.automaticResultError = ''
    verifiedAutomaticContext = undefined
    if (!selected || !selected.processInstanceId || !selected.versionNo) {
      state.loading = false
      state.error = '缺少本次文件版本与申请流程，无法读取发送和签收记录'
      return
    }
    const context = { ...selected }
    const isCurrent = () => {
      const actual = currentContext()
      return attempt === generation && actual?.fileId === context.fileId
        && actual.processInstanceId === context.processInstanceId && actual.versionNo === context.versionNo
        && actual.authorityKey === context.authorityKey
    }
    state.loading = true
    try {
      const [receipts, automatic] = await Promise.allSettled([read(context.fileId), readAutomatic(context.fileId)])
      if (!isCurrent()) return
      try {
        if (receipts.status === 'rejected') throw receipts.reason
        state.rows = validateDistributionFacts(receipts.value, context.fileId, context.versionNo)
      } catch (cause) { state.error = cause instanceof Error ? cause.message : '逐人发送和签收记录读取失败' }
      try {
        if (automatic.status === 'rejected') throw automatic.reason
        const result = automatic.value
        if (!result || !exactResultId(result.controlledFileId) || String(result.controlledFileId) !== context.fileId || result.versionNo !== context.versionNo
          || result.approvalProcessInstanceId !== context.processInstanceId
          || !['NOT_CREATED','PENDING','FAILED','DISPATCH_READY','SENT'].includes(result.status)
          || result.status === 'NOT_CREATED' && result.batchId !== null
          || result.status !== 'NOT_CREATED' && !exactResultId(result.batchId)
          || result.status === 'FAILED' && (!result.errorCode || !result.errorMessage || !result.failureAt)
          || result.status === 'SENT' && !result.sentAt) throw new Error('自动下发结果与本次精确版本或真实失败事实不一致')
        state.automaticResult = { ...result }
        verifiedAutomaticContext = { ...context }
      } catch (cause) { state.automaticResultError = cause instanceof Error ? cause.message : '自动下发结果读取失败' }
    } catch (cause) {
      if (!isCurrent()) return
      state.error = cause instanceof Error ? cause.message : '发送和签收记录读取失败，请刷新后重试'
    } finally {
      if (isCurrent()) state.loading = false
    }
  }
  const retry = async (
    request: AutomaticDistributionRetryRequest,
    send: (command: AutomaticDistributionRetryRequest) => Promise<AutomaticDistributionResult>,
    canRetry: () => boolean
  ): Promise<AutomaticDistributionResult> => {
    const selected = currentContext(), batch = state.automaticResult
    if (!selected || !selected.authorityKey || !canRetry() || state.loading || state.retrying
      || state.automaticResultError || !batch || ['NOT_CREATED', 'SENT'].includes(batch.status)
      || !verifiedAutomaticContext || verifiedAutomaticContext.fileId !== selected.fileId
      || verifiedAutomaticContext.versionNo !== selected.versionNo || verifiedAutomaticContext.processInstanceId !== selected.processInstanceId
      || verifiedAutomaticContext.authorityKey !== selected.authorityKey
      || !exactResultId(request.controlledFileId) || !exactResultId(request.batchId)
      || String(request.controlledFileId) !== selected.fileId || String(request.batchId) !== String(batch.batchId)
      || request.versionNo !== selected.versionNo || request.approvalProcessInstanceId !== selected.processInstanceId)
      throw new Error('请先读取本次有权限的精确批次，等待当前请求完成后再重试')
    const context = { ...selected }, command = { ...request }, attempt = ++generation
    const isCurrent = () => {
      const actual = currentContext()
      return attempt === generation && canRetry() && actual?.fileId === context.fileId
        && actual.processInstanceId === context.processInstanceId && actual.versionNo === context.versionNo
        && actual.authorityKey === context.authorityKey
    }
    state.retrying = true
    activeRetry = attempt
    try {
      const result = await send(command)
      if (!isCurrent()) throw new Error('重试期间账号、权限或文件版本已变化，请重新读取当前下发结果')
      if (!result || !exactResultId(result.controlledFileId) || !exactResultId(result.batchId)
        || String(result.controlledFileId) !== context.fileId || String(result.batchId) !== String(command.batchId)
        || result.versionNo !== context.versionNo || result.approvalProcessInstanceId !== context.processInstanceId
        || !['PENDING', 'FAILED', 'DISPATCH_READY', 'SENT'].includes(result.status)
        || result.status === 'FAILED' && (!result.errorCode || !result.errorMessage || !result.failureAt)
        || result.status === 'SENT' && !result.sentAt)
        throw new Error('重试返回未证明本批的精确状态，未更新发送与签收记录')
      state.automaticResult = { ...result }
      verifiedAutomaticContext = { ...context }
      state.rows = undefined
      return result
    } catch (cause) {
      if (isCurrent()) state.automaticResultError = cause instanceof Error ? cause.message : '自动下发重试失败，请核对正式结果'
      throw cause
    } finally {
      if (activeRetry === attempt) { activeRetry = undefined; state.retrying = false }
    }
  }
  return { state, clear, load, retry }
}
