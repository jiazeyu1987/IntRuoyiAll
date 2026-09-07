const unknown = (code?: string | null) => code ? `未知状态（${code}）` : '未知状态'

const label = (mapping: Record<string, string>, code?: string | null) =>
  code && mapping[code] ? mapping[code] : unknown(code)

export const batchStatusLabel = (code?: string | null) => label({
  PENDING: '待处理', PROCESSING: '通知发送中', READY: '待影响评估',
  PARTIAL_FAILED: '部分通知失败', COMPLETED: '已完成'
}, code)

export const notificationStatusLabel = (code?: string | null) => label({
  PENDING: '待发送', SENT: '已发送', FAILED: '发送失败'
}, code)

export const impactTaskStatusLabel = (code?: string | null) => label({
  PENDING: '待处理', UNASSIGNED: '待文控分配', IN_REVIEW: '评估中', COMPLETED: '已完成'
}, code)

export const impactDecisionLabel = (code?: string | null) => label({
  NO_REVISION_REQUIRED: '无需升版', REVISION_REQUIRED: '需要升版'
}, code)

export const revisionTrackingStatusLabel = (code?: string | null) => label({
  NOT_APPLICABLE: '无需升版', NOT_STARTED: '待开始升版',
  REVISION_LINKED: '已关联大版本', RESOLVED: '已解决'
}, code)

export const visibilitySourceLabel = (code?: string | null) => label({
  FILE_REQUESTER: '发布文件责任人', CURRENT_VIEW_MATRIX: '当前查看权限矩阵',
  PUBLIC_FOLDER_DISTRIBUTION: '正式电子分发'
}, code)

export const visibilityResolutionStatusLabel = (code?: string | null) => label({
  RESOLVED: '已解析', EMPTY: '无匹配用户', UNRESOLVED: '解析失败',
  FILTERED_BY_ASSIGNMENT: '已被项目范围过滤',
  RESOLVED_WITH_ASSIGNMENT_FILTER: '已解析并应用项目范围过滤'
}, code)

export const relationDirectionLabel = (code?: string | null) => label({
  FORWARD: '正向关联', REVERSE: '反向引用'
}, code)

export const controlledFileStatusLabel = (code?: string | null) => label({
  ACTIVE: '当前有效', WORKING: '工作版本', REJECTED: '已驳回',
  PENDING_DOC_CONTROL_REVIEW: '待文控审核', PENDING_MATRIX_REVIEW: '待矩阵审核',
  PENDING_MATRIX_APPROVAL: '待矩阵批准', PENDING_DOC_CONTROL_APPROVAL: '待文控批准',
  PENDING_APPLICANT_REWORK: '待申请人返工', READY_TO_PUBLISH: '待发布',
  FINALIZING: '发布处理中', APPROVING: '审批中'
}, code)
