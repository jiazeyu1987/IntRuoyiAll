import { parseExactIntegerJson } from '@/utils/exactIntegerJson'

const labels: Record<string, string> = {
  categoryCodesJson: '偏差类别',
  investigationMembersJson: '调查成员',
  capaAttachmentsJson: 'CAPA附件',
  fileName: '文件名称',
  fileUrl: '文件地址',
  url: '文件地址',
  nickname: '姓名',
  username: '账号',
  departmentName: '部门',
  rootCauseAnalysis: '根因分析',
  impactScope: '影响范围',
  riskAssessment: '风险评估',
  productDisposition: '产品处置',
  correctiveOwnerName: '整改责任人',
  correctiveDueAt: '整改期限',
  investigationStartedAt: '调查开始时间',
  plannedCompletedAt: '计划完成时间',
  completedAt: '实际完成时间',
  capaRequired: '是否需要CAPA',
  capaCode: 'CAPA编号',
  handlingConclusion: '处理结论',
  verificationResult: '验证结果',
  verificationContent: '验证内容',
  contentVersion: '内容版本',
  adapter: '签署业务类型',
  executionId: '执行记录编号',
  actionType: '业务动作',
  processInstanceId: '审批流程编号',
  bpmTaskId: '审批任务编号',
  bpmTaskDefinitionKey: '审批节点标识',
  bpmTaskName: '审批任务名称',
  signatureCellKey: '签署单元格',
  signatureRowIndex: '签署行位置',
  signatureColumnIndex: '签署列位置',
  reviewSourceType: '复核来源类型',
  reviewSourceId: '复核来源编号',
  reviewSourceName: '复核来源名称',
  approvalResult: '审批结果',
  fieldAuditRevision: '字段审计版本',
  fieldAuditHeadHash: '字段审计摘要',
  cellValuesHash: '表单内容摘要',
  signatureChallengeHash: '签署校验摘要',
  userOpinion: '签署意见',
  moduleCode: '业务模块',
  authenticationMethod: '认证方式',
  eventId: '提交事件编号',
  eventType: '业务类型',
  actorId: '执行人编号',
  operatorId: '设备账号编号',
  tenantId: '租户编号',
  signatureId: '提交签名编号',
  payloadHash: '提交内容摘要',
  reviewSignatureId: '复核签名编号',
  reviewId: '复核记录编号',
  reviewActorId: '复核人编号',
  reviewStatus: '复核状态',
  leaderType: '复核岗位',
  identityDomain: '人员身份类型',
  activeOrderId: '活跃订单编号',
  routeProcessId: '路线工序编号',
  processId: '工序编号',
  submissionIdempotencyKey: '提交唯一标识',
  pqcTaskId: '过程检验任务编号',
  signatureIdentity: '签署身份',
  businessContent: '业务内容',
  subjectId: '业务对象编号',
  subjectType: '业务对象类型',
  subjectVersion: '业务版本',
  actionCode: '业务动作',
  id: '记录编号',
  name: '名称',
  status: '状态',
  reason: '原因',
  remark: '备注',
  workOrderId: '生产订单编号',
  workOrderCode: '生产订单号',
  batchCode: '生产批号',
  batchExecutionId: '批次执行编号',
  routeId: '工艺路线编号',
  routeVersionId: '路线版本编号',
  processName: '工序名称',
  quantity: '数量',
  qualifiedQuantity: '合格数量',
  unqualifiedQuantity: '不合格数量',
  productionQuantity: '生产数量',
  inspectionQuantity: '检验数量',
  submittedQuantity: '提交数量',
  completedQuantity: '完成数量',
  lossQuantity: '损耗数量',
  materialId: '物料编号',
  materialCode: '物料编码',
  materialName: '物料名称',
  materialDetails: '物料明细',
  items: '项目',
  details: '明细',
  result: '结果',
  value: '记录值',
  beforeValue: '修改前',
  afterValue: '修改后',
  fieldName: '字段名称',
  userId: '用户编号',
  employeeId: '员工编号',
  actualEmployeeId: '实际员工编号',
  deviceAccountId: '设备账号编号',
  displayName: '姓名',
  actorDisplayName: '签署人姓名',
  signerName: '签署人',
  reviewerName: '复核人',
  signedAt: '签署时间',
  reviewedAt: '复核时间',
  createTime: '创建时间',
  updateTime: '更新时间',
  submittedAt: '提交时间',
  businessDate: '业务日期',
  version: '版本',
  contentHash: '内容摘要',
  evidenceHash: '证据摘要',
  reviewOpinion: '复核意见',
  decision: '处理决定',
  submissionId: '正式提交编号'
}

const enums: Record<string, Record<string, string>> = {
  categoryCodesJson: {
    PRODUCTION_PROCESS: '生产过程', PRODUCTION_UTILITY: '生产公用系统',
    INSPECTION: '检验', VALIDATION_CONFIRMATION: '验证确认',
    MATERIAL_MANAGEMENT: '物料管理', DOCUMENT_RECORD: '文件记录',
    PRODUCT_RELEASE: '产品放行', OTHER_SYSTEM: '其他系统'
  },
  handlingConclusion: { CLOSED_LOOP: '闭环完成' },
  verificationResult: { PASS: '通过', FAIL: '不通过' },
  eventType: {
    PQC_INSPECTION: '过程质量检验',
    PRODUCTION_SUBMIT: '生产提交',
    PRODUCTION_REPORT: '生产报工'
  },
  identityDomain: { SYSTEM_USER: '系统用户', EMPLOYEE: '员工', MES_EMPLOYEE: '生产员工' },
  leaderType: { PRODUCTION: '生产组长', PQC: 'PQC组长' },
  reviewStatus: {
    PENDING: '待复核',
    APPROVED: '复核通过',
    REJECTED: '复核退回',
    RETURNED: '已退回'
  }
}

export interface EvidenceRow {
  key: string
  label: string
  value: string
}

export const readBusinessEvidence = (source?: string, rootField = '') => {
  if (source == null || source.trim() === '') return { rows: [] as EvidenceRow[], error: '' }
  let value: unknown
  try {
    value = parseExactIntegerJson(source)
  } catch {
    return {
      rows: [] as EvidenceRow[],
      error: '证据内容格式错误，无法展示业务信息。请联系管理员核查原始记录。'
    }
  }
  const rows: EvidenceRow[] = []
  let error = ''
  const visit = (item: unknown, path: string[], names: string[], field: string) => {
    // Cryptographic values remain in the evidence record, never in the business display.
    if (/hash|digest|sha256/i.test(field)) return
    if (typeof item === 'string' && ((/Json$/i.test(field) && path.at(-1) === field) || /^[\[{]/.test(item.trim()))) {
      try {
        item = parseExactIntegerJson(item)
      } catch {
        error = '记录内容格式错误，无法展示业务信息。请联系管理员核查。'
        return
      }
    }
    if (item !== null && typeof item === 'object' && Object.keys(item).length) {
      Object.entries(item).forEach(([key, child]) =>
        visit(
          child,
          [...path, key],
          [
            ...names,
            Array.isArray(item) ? `第 ${Number(key) + 1} 项` : labels[key] || `未标注字段（${key}）`
          ],
          Array.isArray(item) ? field : key
        )
      )
      return
    }
    let text: string
    if (item === null) text = '未记录'
    else if (item === '') text = '空文本'
    else if (typeof item === 'boolean') text = item ? '是' : '否'
    else if (typeof item === 'object') text = Array.isArray(item) ? '无条目' : '无字段'
    else text = enums[field]?.[String(item)] || String(item)
    rows.push({ key: JSON.stringify(path), label: names.join(' / ') || '记录内容', value: text })
  }
  visit(value, [], [], rootField)
  return { rows, error }
}
