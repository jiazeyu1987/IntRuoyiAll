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

export const DCC_APPROVAL_WRONG_PASSWORD_MESSAGE = '当前密码错误，请重新输入后再完成电子签名。'

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
    if (isControlledFileTaskPasswordInvalidError(error)) {
      return {
        success: false,
        field: 'password',
        inlineError: DCC_APPROVAL_WRONG_PASSWORD_MESSAGE
      }
    }
    throw error
  }
}
