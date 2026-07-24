import request from '@/config/axios'

export interface EdhrLabelTemplatePageReqVO extends PageParam {
  templateCode?: string
  templateName?: string
  businessObjectType?: string
  status?: string
  createTime?: string[]
}

export interface EdhrLabelTemplateCreateReqVO {
  templateCode: string
  templateName: string
  templateVersion: string
  businessObjectType: string
  fieldModelJson: string
  layoutJson: string
  parserVersion: string
  watermarkTemplate?: string
  remark?: string
}

export interface EdhrLabelTemplateRespVO {
  id: number
  templateCode?: string
  templateName?: string
  templateVersion?: string
  businessObjectType?: string
  fieldModelJson?: string
  layoutJson?: string
  parserVersion?: string
  watermarkTemplate?: string
  status?: string
  activeAt?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrLabelInstancePageReqVO extends PageParam {
  labelCode?: string
  templateId?: number
  businessType?: string
  businessObjectId?: number
  businessObjectCode?: string
  status?: string
  printStatus?: string
  generatedAt?: string[]
}

export interface EdhrLabelInstanceRespVO {
  id: number
  labelCode?: string
  templateId?: number
  templateCode?: string
  templateVersion?: string
  businessType?: string
  businessObjectId?: number
  businessObjectCode?: string
  renderSnapshotJson?: string
  parserVersion?: string
  status?: string
  printStatus?: string
  businessKeyHash?: string
  generatedBy?: number
  generatedAt?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrLabelPreviewReqVO {
  templateId: number
  businessType: string
  businessObjectId: number
  businessObjectCode: string
  businessObjectPayloadJson: string
}

export interface EdhrLabelPreviewRespVO {
  templateId?: number
  templateCode?: string
  templateVersion?: string
  businessType?: string
  businessObjectId?: number
  businessObjectCode?: string
  parserVersion?: string
  renderSnapshotJson?: string
}

export interface EdhrPrintTaskPageReqVO extends PageParam {
  taskCode?: string
  sourceType?: string
  sourceObjectId?: number
  sourceObjectCode?: string
  templateType?: string
  status?: string
  printConfirmStatus?: string
  isReprint?: boolean
  requestedAt?: string[]
}

export interface EdhrPrintTaskCreateReqVO {
  sourceType: string
  sourceObjectId: number
  sourceObjectCode: string
  templateType: string
  templateId: number
  templateCode: string
  labelInstanceId?: number
  travelerId?: number
  isReprint?: boolean
  originalPrintTaskId?: number
  reprintReason?: string
  watermarkText?: string
  idempotencyKey: string
}

export interface EdhrPrintTaskMarkFailedReqVO {
  id: number
  failureReason: string
}

export interface EdhrPrintTaskConfirmReqVO {
  id: number
  confirmationEvidenceHash: string
}

export interface EdhrPrintTaskRespVO {
  id: number
  taskCode?: string
  sourceType?: string
  sourceObjectId?: number
  sourceObjectCode?: string
  templateType?: string
  templateId?: number
  templateCode?: string
  labelInstanceId?: number
  travelerId?: number
  status?: 'WAITING' | 'PRINTING' | 'PENDING_CONFIRM' | 'SUCCESS_CONFIRMED' | 'FAILED' | 'VOID_RESTRICTED'
  printConfirmStatus?: string
  isReprint?: boolean
  originalPrintTaskId?: number
  reprintReason?: string
  watermarkText?: string
  failureReason?: string
  idempotencyKey?: string
  printCountDeducted?: boolean
  requestedBy?: number
  requestedAt?: string
  confirmedBy?: number
  confirmedAt?: string
  confirmationEvidenceHash?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrPrintPolicyPageReqVO extends PageParam {
  policyCode?: string
  policyName?: string
  businessType?: string
  templateType?: string
  status?: 'DRAFT' | 'ACTIVE' | 'DISABLED'
  createTime?: string[]
}

export interface EdhrPrintPolicyCreateReqVO {
  policyCode: string
  policyName: string
  businessType: string
  templateType: string
  firstPrintLimit: number
  reprintLimit: number
  reasonDictJson: string
  watermarkTemplate: string
  voidCopyWatermark: string
  remark?: string
}

export interface EdhrPrintPolicyRespVO {
  id: number
  policyCode?: string
  policyName?: string
  businessType?: string
  templateType?: string
  firstPrintLimit?: number
  reprintLimit?: number
  reasonDictJson?: string
  watermarkTemplate?: string
  voidCopyWatermark?: string
  status?: 'DRAFT' | 'ACTIVE' | 'DISABLED'
  activeAt?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrReprintApplyReqVO {
  originalPrintTaskId: number
  reprintReasonCode: string
  reprintReason: string
  idempotencyKey: string
}

export interface EdhrReprintRequestRespVO {
  id: number
  requestCode?: string
  printTaskId?: number
  originalPrintTaskId?: number
  reprintReasonCode?: string
  reprintReason?: string
  usedReprintCount?: number
  reprintLimit?: number
  watermarkText?: string
  status?: string
  idempotencyKey?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrPrintHistoryCopyReqVO {
  sourcePrintTaskId: number
  sourceObjectType: string
  sourceObjectCode: string
  copyReason: string
  idempotencyKey: string
}

export interface EdhrPrintHistoryCopyRespVO {
  id: number
  copyCode?: string
  sourcePrintTaskId?: number
  sourceObjectType?: string
  sourceObjectCode?: string
  copyReason?: string
  watermarkText?: string
  evidenceHash?: string
  idempotencyKey?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrPrintHistoryExportReqVO {
  filterSnapshotJson: string
  idempotencyKey: string
}

export interface EdhrPrintExportAuditRespVO {
  id: number
  exportCode?: string
  filterSnapshotJson?: string
  resultStatus?: string
  evidenceHash?: string
  idempotencyKey?: string
  exportedBy?: number
  exportedAt?: string
  createTime?: string
  updateTime?: string
}

export const getEdhrLabelTemplatePage = async (params: EdhrLabelTemplatePageReqVO) =>
  await request.get<PageResult<EdhrLabelTemplateRespVO[]>>({
    url: '/mes/pro/edhr-label-template/page',
    params
  })

export const createEdhrLabelTemplate = async (data: EdhrLabelTemplateCreateReqVO) =>
  await request.post<EdhrLabelTemplateRespVO>({
    url: '/mes/pro/edhr-label-template/create',
    data
  })

export const activateEdhrLabelTemplate = async (id: number) =>
  await request.post<EdhrLabelTemplateRespVO>({
    url: '/mes/pro/edhr-label-template/activate',
    data: { id }
  })

export const getEdhrLabelPage = async (params: EdhrLabelInstancePageReqVO) =>
  await request.get<PageResult<EdhrLabelInstanceRespVO[]>>({
    url: '/mes/pro/edhr-label/page',
    params
  })

export const previewLabel = async (data: EdhrLabelPreviewReqVO) =>
  await request.post<EdhrLabelPreviewRespVO>({
    url: '/mes/pro/edhr-label/preview',
    data
  })

export const getEdhrPrintTaskPage = async (params: EdhrPrintTaskPageReqVO) =>
  await request.get<PageResult<EdhrPrintTaskRespVO[]>>({
    url: '/mes/pro/edhr-print-task/page',
    params
  })

export const createPrintTask = async (data: EdhrPrintTaskCreateReqVO) =>
  await request.post<EdhrPrintTaskRespVO>({
    url: '/mes/pro/edhr-print-task/create',
    data
  })

export const markPrintTaskFailed = async (data: EdhrPrintTaskMarkFailedReqVO) =>
  await request.post<EdhrPrintTaskRespVO>({
    url: '/mes/pro/edhr-print-task/mark-failed',
    data
  })

export const confirmPrintTask = async (data: EdhrPrintTaskConfirmReqVO) =>
  await request.post<EdhrPrintTaskRespVO>({
    url: '/mes/pro/edhr-print-task/confirm',
    data
  })

export const getEdhrPrintPolicyPage = async (params: EdhrPrintPolicyPageReqVO) =>
  await request.get<PageResult<EdhrPrintPolicyRespVO[]>>({
    url: '/mes/pro/edhr-print-policy/page',
    params
  })

export const createEdhrPrintPolicy = async (data: EdhrPrintPolicyCreateReqVO) =>
  await request.post<EdhrPrintPolicyRespVO>({
    url: '/mes/pro/edhr-print-policy/create',
    data
  })

export const activateEdhrPrintPolicy = async (id: number) =>
  await request.post<EdhrPrintPolicyRespVO>({
    url: '/mes/pro/edhr-print-policy/activate',
    data: { id }
  })

export const applyReprint = async (data: EdhrReprintApplyReqVO) =>
  await request.post<EdhrReprintRequestRespVO>({
    url: '/mes/pro/edhr-print-task/reprint/apply',
    data
  })

export const createVoidHistoryCopy = async (data: EdhrPrintHistoryCopyReqVO) =>
  await request.post<EdhrPrintHistoryCopyRespVO>({
    url: '/mes/pro/edhr-print-task/history-copy',
    data
  })

export const exportPrintHistory = async (data: EdhrPrintHistoryExportReqVO) =>
  await request.post<EdhrPrintExportAuditRespVO>({
    url: '/mes/pro/edhr-print-task/export-history',
    data
  })

export const EdhrLabelPrintApi = {
  getLabelTemplatePage: getEdhrLabelTemplatePage,
  createLabelTemplate: createEdhrLabelTemplate,
  activateLabelTemplate: activateEdhrLabelTemplate,
  getLabelPage: getEdhrLabelPage,
  previewLabel,
  getPrintTaskPage: getEdhrPrintTaskPage,
  createPrintTask,
  markPrintTaskFailed,
  confirmPrintTask,
  getEdhrPrintPolicyPage,
  createEdhrPrintPolicy,
  activateEdhrPrintPolicy,
  applyReprint,
  createVoidHistoryCopy,
  exportPrintHistory
}
