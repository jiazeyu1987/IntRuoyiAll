import {
  approveControlledFileTask,
  DccTaskActionError,
  isControlledFileTaskPasswordInvalidError,
  rejectControlledFileTask,
  type ControlledFileApproveTaskReqVO,
  type ControlledFileDistributionScopeVO,
  type ControlledFileRejectTaskReqVO,
  type DccSignatureActionRespVO
} from '@/api/dcc/controlledFile/workflow'
import { getDccControlledFileStageByKey, type DccControlledFileStageKey } from '../shared/lifecycle'

export type DccApprovalActionMode = 'approve' | 'reject'

export interface DccApprovalActionForm {
  password: string
  reason: string
  sessionId?: string
  stampedPdfUploadTicket?: string
  confirmedDirectoryId?: number
  selectedDistributionScopes?: ControlledFileDistributionScopeVO[]
}

export interface DccApprovalActionValidationErrors {
  password?: string
  reason?: string
}

export interface DccApprovalActionSubmitResult {
  success: boolean
  field?: keyof DccApprovalActionValidationErrors
  inlineError?: string
  response?: DccSignatureActionRespVO
}

export const DCC_APPROVAL_WRONG_PASSWORD_MESSAGE =
  '签名失败原因：当前密码错误。处理建议：请重新输入当前账号密码；如仍失败，请联系文控或系统管理员确认账号状态。责任入口：当前签名人 / 文控负责人。'
const DCC_APPROVAL_SIGNATURE_AUTH_MESSAGE =
  '签名失败原因：电子签名未授权，当前账号缺少该节点电子签名授权或授权策略未生效。处理建议：请联系文控负责人开通当前节点电子签名授权，或由流程管理员确认审批候选人配置。责任入口：DCC 电子签名管理 / 文控负责人。'
const DCC_APPROVAL_SIGNATURE_IMAGE_MESSAGE =
  '签名失败原因：签名图片失效，当前账号签名图片缺失、停用或校验失败。处理建议：请在 DCC 电子签名管理中重新维护签名图片后再提交。责任入口：DCC 电子签名管理 / 文控负责人。'
const DCC_APPROVAL_SIGNATURE_EVIDENCE_MESSAGE =
  '签名失败原因：证据快照失败，电子签名证据、哈希或快照生成失败，本次审批不会推进。处理建议：请联系文控负责人确认文件证据和签名服务状态后重试。责任入口：DCC 电子签名管理 / 文控负责人。'
const DCC_APPROVAL_POST_REQUIRED_MESSAGE =
  '审批人未配置系统岗位，请先在系统用户配置中为当前审批人分配有效岗位。'
const DCC_APPROVAL_ROUTE_RUNTIME_MISMATCH_MESSAGE =
  '审批路线快照与当前实际任务分配不一致，请联系流程管理员检查任务分配后再审批。'

const appendDccApprovalErrorDetail = (message: string, detail: string) => {
  const normalizedDetail = detail.trim()
  if (!normalizedDetail || normalizedDetail === message || normalizedDetail.includes(message)) {
    return message
  }
  return `${message} 原因：${normalizedDetail}`
}

const resolveDccApprovalErrorText = (error: unknown, fallback: string) => {
  if (error instanceof DccTaskActionError && error.message) {
    return error.message
  }
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

export const resolveDccApprovalSignatureErrorMessage = (
  error: unknown,
  fallback = '签名提交失败，请查看错误提示后重试。'
) => {
  if (isControlledFileTaskPasswordInvalidError(error)) {
    return DCC_APPROVAL_WRONG_PASSWORD_MESSAGE
  }
  const rawMessage = resolveDccApprovalErrorText(error, fallback)
  const normalized = rawMessage.toLowerCase()
  if (/1080000199|审批人未配置系统岗位/.test(normalized)) {
    return DCC_APPROVAL_POST_REQUIRED_MESSAGE
  }
  if (/1080000201|审批路线快照与实际任务分配不一致|路线运行态不一致/.test(normalized)) {
    return DCC_APPROVAL_ROUTE_RUNTIME_MISMATCH_MESSAGE
  }
  if (/signature[_\s-]*image|签名图片|签章图片|image.*invalid|image.*missing|image.*expired|image.*disabled/.test(normalized)) {
    return appendDccApprovalErrorDetail(DCC_APPROVAL_SIGNATURE_IMAGE_MESSAGE, rawMessage)
  }
  if (/unauthori[sz]ed|permission|policy|authorization|auth|locked|未授权|无权限|授权|策略|锁定/.test(normalized)) {
    return appendDccApprovalErrorDetail(DCC_APPROVAL_SIGNATURE_AUTH_MESSAGE, rawMessage)
  }
  if (/evidence|snapshot|hash|persist|proof|证据|快照|哈希|hash|留痕/.test(normalized)) {
    return appendDccApprovalErrorDetail(DCC_APPROVAL_SIGNATURE_EVIDENCE_MESSAGE, rawMessage)
  }
  return rawMessage || fallback
}

export const getDccApprovalActionLabels = (stageCode?: string) => {
  const stage = getDccControlledFileStageByKey(stageCode as DccControlledFileStageKey | undefined)
  const approveText = stage?.requiredPermission === 'APPROVE' ? '批准通过' : '审核通过'
  return {
    approveText,
    rejectText: '驳回',
    dialogTitle: `${stage?.label || 'DCC 审批'}签名`
  }
}

export const getDccApprovalSignatureMeaningPreview = (
  stageCode: string | undefined,
  action: DccApprovalActionMode
) => {
  const stage = getDccControlledFileStageByKey(stageCode as DccControlledFileStageKey | undefined)
  const actionText = action === 'approve' ? '通过' : '驳回'
  return `${stage?.label || '当前审批'}${actionText}`
}

export const validateDccApprovalActionForm = (
  mode: DccApprovalActionMode,
  form: DccApprovalActionForm
): DccApprovalActionValidationErrors => {
  const errors: DccApprovalActionValidationErrors = {}
  if (!form.password?.trim()) {
    errors.password = '请输入登录密码完成电子签名'
  }
  if (mode === 'reject' && !form.reason?.trim()) {
    errors.reason = '请输入驳回原因'
  }
  return errors
}

const assertDccSignatureResponseForFile = (
  fileId: number | string,
  response: DccSignatureActionRespVO
) => {
  if (String(response.controlledFileId) !== String(fileId)) {
    throw new DccTaskActionError(
      `文件签名响应文件不匹配：当前文件 ${String(fileId)}，响应文件 ${String(
        response.controlledFileId
      )}`
    )
  }
}

export const submitDccApprovalAction = async ({
  fileId,
  action,
  form,
  taskId
}: {
  fileId: number | string
  action: DccApprovalActionMode
  form: DccApprovalActionForm
  taskId: string
}): Promise<DccApprovalActionSubmitResult> => {
  const errors = validateDccApprovalActionForm(action, form)
  if (errors.password) {
    return {
      success: false,
      field: 'password',
      inlineError: errors.password
    }
  }
  if (errors.reason) {
    return {
      success: false,
      field: 'reason',
      inlineError: errors.reason
    }
  }

  try {
    let response: DccSignatureActionRespVO
    if (action === 'approve') {
      const payload: ControlledFileApproveTaskReqVO = {
        taskId,
        password: form.password,
        reason: form.reason?.trim() || '',
        sessionId: form.sessionId,
        stampedPdfUploadTicket: form.stampedPdfUploadTicket,
        confirmedDirectoryId: form.confirmedDirectoryId,
        selectedDistributionScopes: form.selectedDistributionScopes
      }
      response = await approveControlledFileTask(fileId, payload)
    } else {
      const payload: ControlledFileRejectTaskReqVO = {
        taskId,
        password: form.password,
        reason: form.reason.trim()
      }
      response = await rejectControlledFileTask(fileId, payload)
    }
    assertDccSignatureResponseForFile(fileId, response)
    return { success: true, response }
  } catch (error) {
    if (error instanceof DccTaskActionError) {
      return {
        success: false,
        field: isControlledFileTaskPasswordInvalidError(error) ? 'password' : undefined,
        inlineError: resolveDccApprovalSignatureErrorMessage(error)
      }
    }
    throw error
  }
}
