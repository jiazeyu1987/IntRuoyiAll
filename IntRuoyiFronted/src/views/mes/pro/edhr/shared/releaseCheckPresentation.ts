import type { EdhrReleaseEventType, EdhrReleaseStatus } from '@/api/mes/pro/edhr/release'
import type {
  EdhrOperationAuditPermissionDecision,
  EdhrOperationAuditResultStatus
} from '@/api/mes/pro/edhr/operationAudit'

const RELEASE_CHECK_RESULT_LABELS: Record<string, string> = {
  PASS: '通过',
  FAIL: '失败',
  BLOCKER: '阻塞',
  NOT_APPLICABLE: '不适用',
  PRECHECK_REQUIRED: '待检'
}

const RELEASE_CHECK_CODE_LABELS: Record<string, string> = {
  DHR_COMPLETENESS: '批记录完整性检查',
  INSPECTION_RESULT: '检验结果检查',
  DEVIATION_CLOSED: '偏差关闭检查',
  REWORK_CLOSED: '返工完成检查',
  SCRAP_RECORDED: '报废记录检查',
  INVENTORY_CONSISTENCY: '库存一致性检查'
}

const RELEASE_CHECK_CATEGORY_LABELS: Record<string, string> = {
  DHR: '批记录完整性',
  INSPECTION: '检验',
  DEVIATION: '偏差',
  REWORK: '返工',
  SCRAP: '报废',
  INVENTORY: '库存'
}

const RELEASE_CHECK_SOURCE_OBJECT_TYPE_LABELS: Record<string, string> = {
  EDHR_BATCH_EXECUTION: '批次执行记录',
  DHR: '批记录',
  INSPECTION: '检验记录',
  DEVIATION: '偏差记录',
  REWORK: '返工记录',
  SCRAP: '报废记录',
  INVENTORY: '库存记录'
}

const RELEASE_EVENT_LABELS: Record<EdhrReleaseEventType, string> = {
  PRECHECK: '预检',
  SUBMIT: '提交',
  APPROVE: '批准',
  REJECT: '驳回',
  WITHDRAW: '撤回'
}

const RELEASE_STATUS_LABELS: Record<EdhrReleaseStatus, string> = {
  PRECHECK_REQUIRED: '待预检',
  PRECHECK_FAILED: '预检失败',
  PRECHECK_PASSED: '预检通过',
  PENDING_APPROVAL: '待审批',
  RELEASED: '已放行',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回'
}

export const OPERATION_AUDIT_RESULT_OPTIONS: Array<{ label: string; value: EdhrOperationAuditResultStatus }> = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '拒绝', value: 'REJECTED' }
]

export const OPERATION_AUDIT_PERMISSION_DECISION_OPTIONS: Array<{
  label: string
  value: EdhrOperationAuditPermissionDecision
}> = [
  { label: '允许', value: 'ALLOW' },
  { label: '拒绝', value: 'DENY' }
]

const OPERATION_AUDIT_OBJECT_TYPE_LABELS: Record<string, string> = {
  BATCH_EXECUTION_PAGE: '批次执行页面',
  BATCH_EXECUTION: '批次执行',
  BATCH_EXECUTION_TASK: '批次执行任务',
  BATCH_RECORD_EXECUTION: '批记录执行',
  BATCH_ARCHIVE: '批次归档',
  EXECUTION_ARCHIVE_PAGE: '归档页面',
  CONTROLLED_FILE: '受控文件',
  FORM_TEMPLATE: '表单模板',
  DHR_TEMPLATE: '电子批记录模板',
  RECORDBOOK_TEMPLATE: '记录册模板',
  BATCH_RECORD_VERSION: '批记录版本',
  ROUTE_PROCESS_BATCH_RECORD: '工序批记录配置',
  SPECIAL_NODE_ATTACHMENT: '特殊节点附件',
  WORK_TASK_ASSIGNMENT_RULE: '工作任务规则',
  WORK_TASK: '工作任务',
  RELEASE_TRANSACTION: '放行事务'
}

const OPERATION_AUDIT_OPERATION_TYPE_LABELS: Record<string, string> = {
  QUERY: '查询',
  VIEW: '查看',
  OPEN: '打开',
  LIST: '列表查询',
  SKIP: '跳过',
  COMPLETE: '完成',
  SYNC: '同步',
  CLOSE: '关闭',
  QUALITY_REJECT: '质量拒收',
  ARCHIVE: '归档',
  DOWNLOAD: '下载',
  FIELD_CHANGE: '字段变更',
  VERIFY: '校验',
  EXPORT: '导出',
  EXPORT_AUDIT: '导出审计',
  PERMISSION_RULE_SAVE: '保存权限规则',
  PERMISSION_EVALUATE: '权限评估',
  LOCAL_STATE_SAMPLE_CREATE: '本地状态样本创建',
  ATTACHMENT_PREPARE_UPLOAD: '附件上传预登记',
  ATTACHMENT_PENDING_DELETE: '待提交附件删除',
  ATTACHMENT_SAVE_PENDING: '待提交附件保存',
  WORK_TASK_RULE_SAVE: '工作任务规则保存',
  CANDIDATE_SIGNATURE_COMPLETE: '候选签名完成',
  FILL_TASK_REASSIGN: '填写任务重新派发',
  PRECHECK: '预检'
}

const normalizePresentationKey = (value?: string) => value?.trim().toUpperCase()

export const resolveReleaseCheckResultLabel = (status?: string) => {
  const normalizedStatus = normalizePresentationKey(status)
  if (!normalizedStatus) return '待检'
  return RELEASE_CHECK_RESULT_LABELS[normalizedStatus] || '未知结果'
}

export const resolveReleaseCheckCodeLabel = (checkCode?: string) => {
  const normalizedCode = normalizePresentationKey(checkCode)
  if (!normalizedCode) return '未知检查项'
  return RELEASE_CHECK_CODE_LABELS[normalizedCode] || '未知检查项'
}

export const resolveReleaseCheckCategoryLabel = (checkCategory?: string) => {
  const normalizedCategory = normalizePresentationKey(checkCategory)
  if (!normalizedCategory) return '未知分类'
  return RELEASE_CHECK_CATEGORY_LABELS[normalizedCategory] || '未知分类'
}

export const resolveReleaseCheckSourceObjectTypeLabel = (sourceObjectType?: string) => {
  const normalizedSourceObjectType = normalizePresentationKey(sourceObjectType)
  if (!normalizedSourceObjectType) return '未关联来源'
  return RELEASE_CHECK_SOURCE_OBJECT_TYPE_LABELS[normalizedSourceObjectType] || '未知来源'
}

export const resolveReleaseEventLabel = (eventType?: EdhrReleaseEventType | string) => {
  const normalizedEventType = normalizePresentationKey(eventType)
  if (!normalizedEventType) return '未知事件'
  return RELEASE_EVENT_LABELS[normalizedEventType as EdhrReleaseEventType] || '未知事件'
}

export const resolveReleaseStatusLabel = (status?: EdhrReleaseStatus | string) => {
  const normalizedStatus = normalizePresentationKey(status)
  if (!normalizedStatus) return '未知状态'
  return RELEASE_STATUS_LABELS[normalizedStatus as EdhrReleaseStatus] || '未知状态'
}

export const resolveReleaseCheckResultTagType = (status?: string) => {
  const normalizedStatus = normalizePresentationKey(status)
  if (normalizedStatus === 'PASS') return 'success'
  if (normalizedStatus === 'FAIL' || normalizedStatus === 'BLOCKER') return 'danger'
  if (normalizedStatus === 'NOT_APPLICABLE' || normalizedStatus === 'PRECHECK_REQUIRED' || !normalizedStatus) return 'info'
  return 'warning'
}

export const resolveReleaseTagType = (status?: EdhrReleaseStatus | string) => {
  const normalizedStatus = normalizePresentationKey(status)
  if (normalizedStatus === 'PRECHECK_FAILED' || normalizedStatus === 'REJECTED') return 'danger'
  if (normalizedStatus === 'PRECHECK_PASSED' || normalizedStatus === 'RELEASED') return 'success'
  if (normalizedStatus === 'PENDING_APPROVAL') return 'warning'
  return 'info'
}

export const resolveOperationAuditObjectTypeLabel = (objectType?: string) => {
  const normalizedType = normalizePresentationKey(objectType)
  if (!normalizedType) return '未知对象'
  return OPERATION_AUDIT_OBJECT_TYPE_LABELS[normalizedType] || '未知对象'
}

export const resolveOperationTypeLabel = (operationType?: string) => {
  const normalizedType = normalizePresentationKey(operationType)
  if (!normalizedType) return '未知动作类型'
  return OPERATION_AUDIT_OPERATION_TYPE_LABELS[normalizedType] || '未知动作类型'
}

export const resolveOperationActionLabel = (row?: { actionName?: string; operationType?: string }) => {
  const actionName = row?.actionName?.trim()
  if (actionName) return actionName
  return resolveOperationTypeLabel(row?.operationType)
}

export const resolveOperationAuditResultStatusLabel = (resultStatus?: EdhrOperationAuditResultStatus | string) => {
  const normalizedStatus = normalizePresentationKey(resultStatus)
  const matched = OPERATION_AUDIT_RESULT_OPTIONS.find((option) => option.value === normalizedStatus)
  return matched?.label || '未知结果'
}

export const resolveOperationAuditPermissionDecisionLabel = (
  permissionDecision?: EdhrOperationAuditPermissionDecision | string
) => {
  const normalizedDecision = normalizePresentationKey(permissionDecision)
  const matched = OPERATION_AUDIT_PERMISSION_DECISION_OPTIONS.find((option) => option.value === normalizedDecision)
  return matched?.label || '未知权限决策'
}

export const resolveOperationAuditResultTagType = (resultStatus?: EdhrOperationAuditResultStatus | string) => {
  const normalizedStatus = normalizePresentationKey(resultStatus)
  if (normalizedStatus === 'SUCCESS') return 'success'
  if (normalizedStatus === 'REJECTED') return 'warning'
  if (normalizedStatus === 'FAILED') return 'danger'
  return 'info'
}

export const resolveOperationAuditPermissionDecisionTagType = (
  permissionDecision?: EdhrOperationAuditPermissionDecision | string
) => {
  return normalizePresentationKey(permissionDecision) === 'DENY' ? 'danger' : 'success'
}
