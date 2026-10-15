import request from '@/config/axios'
import type { ApprovalMatrixBaselineVO, ApprovalMatrixConfirmationSelection } from './workflow'
import type {
  ProjectAttributes,
  AttributeSnapshot
} from '@/views/dcc/controlled-file/project-attributes/state'
import { validateAttributes } from '@/views/dcc/controlled-file/project-attributes/state'
import type {
  FileCandidate,
  SelectorPage,
  SelectorQuery
} from '@/views/dcc/controlled-file/relations/selector-state'
import type { RevisionTarget } from '@/views/dcc/controlled-file/revision/revision-model'

type Identity = number | string
export type DccApplicationType = 'UPLOAD' | 'REVISION' | 'OBSOLETE'
export type DccRoundFlowRule =
  | 'LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL'
  | 'PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL'
  | 'PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM'
  | 'PUBLIC_V5_OBSOLETE_APPROVAL_END'
export interface ControlledFileRoundFlowContract {
  tenantId: Identity
  controlledFileId: Identity
  applicationType: DccApplicationType
  bpmRound: string
  attributeRound: number
  status: 'READY' | 'NOT_RECORDED' | 'UNSUPPORTED_DEFINITION'
  unavailableReason: string | null
  processDefinitionKey: string
  processDefinitionId: string
  deploymentId: string
  definitionDigest: string
  executionContract: 'PUBLIC_MATRIX_NATIVE_V1' | 'PUBLIC_MATRIX_NATIVE_V2' | 'LEGACY_V4_ACTION' | null
  flowRule: DccRoundFlowRule | null
  needTraining: boolean | null
  formInstanceId: Identity | null
  formInstanceCode: string | null
}
export interface ControlledFileApplicationRound {
  controlledFileId: string
  applicationType: DccApplicationType
  bpmRound: string
  attributeRound: number
  flowContract?: ControlledFileRoundFlowContract | null
}
export interface ControlledFileApplicationProgress extends ControlledFileApplicationRound {
  tasks: Array<{ id: string; processInstanceId: string; taskDefinitionKey: string; status: number;
    assignee?: string | null; reason?: string | null; createTime?: string | readonly number[] | null; endTime?: string | readonly number[] | null }>
}
export const getControlledFileApplicationProgress = async (fileId: Identity, bpmRound: string): Promise<ControlledFileApplicationProgress> => {
  const id = identity(fileId)
  if (!bpmRound || bpmRound !== bpmRound.trim()) throw new Error('培训申请轮次身份无效')
  const result = await request.get<ControlledFileApplicationProgress>({
    url: `/dcc/controlled-files/${id}/training-application-progress`, params: { bpmRound }, ignoreErrorMessage: true
  })
  if (!result || identity(result.controlledFileId) !== id || result.bpmRound !== bpmRound
    || !['UPLOAD', 'REVISION'].includes(result.applicationType) || !Number.isSafeInteger(result.attributeRound)
    || result.attributeRound < 1 || !Array.isArray(result.tasks) || !result.tasks.length
    || result.tasks.some(task => !task.id || task.processInstanceId !== bpmRound || !task.taskDefinitionKey || !Number.isInteger(task.status)))
    throw new Error('培训申请进度响应身份或任务事实缺失')
  validateControlledFileRoundFlowContract(result)
  return result
}
export interface ControlledFileRelationPermissions {
  hasCurrentControlledSource: boolean
  latestControlledFileId?: string | null
  currentActiveControlledFileId?: string | null
  controlledFileId: string
  tenantId: string
  masterId: string
  projectId: string
  projectName: string
  projectFolderId: string | null
  projectFolderName: string | null
  fileNumber: string
  fileName: string
  versionNo: string
  status: string
  controlled: boolean
  pendingEffect: boolean
  executable: boolean
  canEdit: boolean
  canPreview: boolean
}
export interface WorkingApplicationAttributes extends AttributeSnapshot {
  approvalSourceMode?: 'PUBLIC_MATRIX' | 'APPLICATION_CUSTOM' | null
  applicationCustomMatrix?: import('./workflow').ApplicationCustomMatrix | null
  applicationContextKey?: string | null
  attributeRound?: number | null
  applicationCustomMatrixDigest?: string | null
  executionContract?: 'PUBLIC_MATRIX_NATIVE_V1' | 'PUBLIC_MATRIX_NATIVE_V2' | 'LEGACY_V4_ACTION' | null
  preparedProcessDefinitionId?: string | null
  preparedDefinitionDigest?: string | null
  newActionBaselineRequired?: boolean
  newActionBaselinePreparationAllowed?: boolean
  draftApprovalMatrix?: ApprovalMatrixBaselineVO | null
  controlledFileId: string
  effectiveDate: string | null
  effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE' | 'DELAYED' | 'PRESET_DATE' | null
  needTraining: boolean | null
  selectedSignoffDepartmentIds: string[] | null
  selectedDistributionDepartmentIds: string[] | null
  changeDescription: string | null
  canSubmit: boolean
  unavailableReason: string | null
}
export interface WorkingApplicationSelection extends Omit<ApprovalMatrixConfirmationSelection, 'selectedSignoffDepartmentIds'> {
  prepareNewActionBaseline?: boolean | null
  effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE' | 'DELAYED' | 'PRESET_DATE' | null
  effectiveDate: string | null
  selectedSignoffDepartmentIds: Identity[] | null
  selectedDistributionDepartmentIds: Identity[] | null
  needTraining: boolean | null
  changeDescription: string | null
}

export interface DelayedEffectiveConfirmationContext {
  controlledFileId: string
  taskId: string
  processInstanceId: string
  attributeRound: number
  processDefinitionId: string
  definitionDigest: string
  versionNo: string
  effectiveMode: 'DELAYED'
  stageCode: 'DELAYED_EFFECTIVE_CONFIRM'
  canConfirm: boolean
  unavailableReason?: string | null
  timeZone: 'Asia/Shanghai'
  serverTime: string | number
}
export interface DelayedEffectiveConfirmationCommand {
  taskId: string; processInstanceId: string; attributeRound: number
  expectedProcessDefinitionId: string; expectedDefinitionDigest: string
  selectedEffectiveTime: string; password: string; reason: string; idempotencyKey: string
}
export interface DelayedEffectiveConfirmationResult {
  controlledFileId: string; taskId: string; processInstanceId: string; attributeRound: number
  selectedEffectiveTime: string; signatureId: string; evidenceHash: string
  signedAt: string | number; status: 'CONFIRMED'
}
const exactConfirmationText = (value: unknown, name: string): string => {
  if (typeof value !== 'string' || !value || value !== value.trim() || /[\x00-\x1f\x7f]/.test(value))
    throw new Error(`延迟生效确认${name}无效`)
  return value
}
/** Fixed Shanghai civil time. Never parse through browser local timezone or round real signedAt. */
const delayedTimeNanos = (value: unknown, selected: boolean): bigint => {
  if (!selected && typeof value === 'number' && Number.isSafeInteger(value)) return BigInt(value) * 1000000n
  if (typeof value !== 'string') throw new Error('延迟生效确认时间格式无效')
  const parts = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?$/.exec(value)
  if (!parts || (selected && parts[7])) throw new Error('请选择有效生效时间（YYYY-MM-DD HH:mm:ss）')
  const [year, month, day, hour, minute, second] = parts.slice(1, 7).map(Number)
  const date = new Date(0)
  date.setUTCFullYear(year, month - 1, day); date.setUTCHours(hour, minute, second, 0)
  if (year < 1 || date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day
      || date.getUTCHours() !== hour || date.getUTCMinutes() !== minute || date.getUTCSeconds() !== second)
    throw new Error('延迟生效确认时间不是有效日历时间')
  return BigInt(date.getTime() - 8 * 3600000) * 1000000n + BigInt((parts[7] || '').padEnd(9, '0'))
}
export const validateDelayedEffectiveConfirmationContext = (value: DelayedEffectiveConfirmationContext): DelayedEffectiveConfirmationContext => {
  if (!value || value.effectiveMode !== 'DELAYED' || value.stageCode !== 'DELAYED_EFFECTIVE_CONFIRM'
      || value.timeZone !== 'Asia/Shanghai' || typeof value.canConfirm !== 'boolean'
      || !Number.isSafeInteger(value.attributeRound) || value.attributeRound < 1
      || !/^[a-f0-9]{64}$/.test(value.definitionDigest)) throw new Error('延迟生效确认任务合同不完整')
  identity(value.controlledFileId)
  for (const key of ['taskId', 'processInstanceId', 'processDefinitionId', 'versionNo'] as const)
    exactConfirmationText(value[key], key)
  delayedTimeNanos(value.serverTime, false)
  return value
}
export const buildDelayedEffectiveConfirmationCommand = (context: DelayedEffectiveConfirmationContext,
  selectedEffectiveTime: string, password: string, reason: string, idempotencyKey: string): DelayedEffectiveConfirmationCommand => {
  validateDelayedEffectiveConfirmationContext(context)
  if (!context.canConfirm) throw new Error(context.unavailableReason || '当前用户不可办理本轮延迟生效确认')
  if (delayedTimeNanos(selectedEffectiveTime, true) < delayedTimeNanos(context.serverTime, false))
    throw new Error('生效时间不得早于本次确认时间，请重新选择')
  if (!password || !password.trim()) throw new Error('请输入电子签名密码')
  if (!reason || !reason.trim() || reason.trim().length > 500) throw new Error('请填写500字符以内的真实电子签名原因')
  exactConfirmationText(idempotencyKey, '幂等身份')
  return { taskId: context.taskId, processInstanceId: context.processInstanceId, attributeRound: context.attributeRound,
    expectedProcessDefinitionId: context.processDefinitionId, expectedDefinitionDigest: context.definitionDigest,
    selectedEffectiveTime, password, reason: reason.trim(), idempotencyKey }
}
export const validateDelayedEffectiveConfirmationResult = (context: DelayedEffectiveConfirmationContext,
  command: DelayedEffectiveConfirmationCommand, value: DelayedEffectiveConfirmationResult): DelayedEffectiveConfirmationResult => {
  validateDelayedEffectiveConfirmationContext(context)
  if (!value || identity(value.controlledFileId) !== identity(context.controlledFileId)
      || value.taskId !== context.taskId || value.taskId !== command.taskId
      || value.processInstanceId !== context.processInstanceId || value.processInstanceId !== command.processInstanceId
      || value.attributeRound !== context.attributeRound || value.attributeRound !== command.attributeRound
      || value.selectedEffectiveTime !== command.selectedEffectiveTime || value.status !== 'CONFIRMED'
      || !/^[a-f0-9]{64}$/.test(value.evidenceHash)) throw new Error('延迟生效确认结果与本次任务或签名证据不一致')
  identity(value.signatureId)
  if (delayedTimeNanos(value.selectedEffectiveTime, true) < delayedTimeNanos(value.signedAt, false))
    throw new Error('所选生效时间早于真实签名时间，确认结果无效')
  return value
}
export const getDelayedEffectiveConfirmation = async (fileId: Identity, taskId: string, processInstanceId: string,
  attributeRound: number): Promise<DelayedEffectiveConfirmationContext> => {
  const id = identity(fileId)
  exactConfirmationText(taskId, '任务身份'); exactConfirmationText(processInstanceId, '轮次身份')
  if (!Number.isSafeInteger(attributeRound) || attributeRound < 1) throw new Error('延迟生效确认属性轮次无效')
  const value = await request.get<DelayedEffectiveConfirmationContext>({ url: `/dcc/controlled-files/${id}/delayed-effective-confirm`,
    params: { taskId, processInstanceId, attributeRound }, ignoreErrorMessage: true })
  validateDelayedEffectiveConfirmationContext(value)
  if (identity(value.controlledFileId) !== id || value.taskId !== taskId || value.processInstanceId !== processInstanceId
      || value.attributeRound !== attributeRound) throw new Error('延迟生效确认响应任务身份不一致')
  return value
}
export const confirmDelayedEffectiveness = async (context: DelayedEffectiveConfirmationContext,
  command: DelayedEffectiveConfirmationCommand): Promise<DelayedEffectiveConfirmationResult> => {
  const validated = buildDelayedEffectiveConfirmationCommand(context, command.selectedEffectiveTime, command.password, command.reason, command.idempotencyKey)
  if (JSON.stringify(validated) !== JSON.stringify(command)) throw new Error('延迟生效确认请求与精确任务合同不一致')
  const value = await request.post<DelayedEffectiveConfirmationResult>({ url: `/dcc/controlled-files/${identity(context.controlledFileId)}/delayed-effective-confirm`,
    data: validated, ignoreErrorMessage: true })
  return validateDelayedEffectiveConfirmationResult(context, validated, value)
}
export interface SignoffAssignmentContext {
  controlledFileId: string
  processInstanceId: string
  taskId: string
  processDefinitionKey: 'dcc-controlled-file-upload' | 'dcc-controlled-file-revision' | 'dcc-controlled-file-obsolete'
  obligationId: string
  departmentId: string
  departmentName: string | null
  assigneeUserId: string | null
  assigned: boolean
  canAssign: boolean
  assigneeOptions: Array<{ id: string; name: string }>
}
export interface ControlledFileRevisionOptions {
  controlledBaselineId: Identity
  baselineVersionNo: string
  masterId: Identity
  sourceOriginalFileName: string
  partialTarget: RevisionTarget
  replacementTarget: RevisionTarget
  checkedOutBy: Identity | null
  checkedOutByName: string | null
  checkedOutTime: string | number | null
  lockedReason: string | null
  iterations: Array<{
    id: Identity
    versionNo: string
    canPartial: boolean
    canReplacement: boolean
    canPreview: boolean
    createTime: string | number | null
    changeDescription: string | null
  }>
}
export interface ControlledFileApplicationEvidence {
  nodeRecords: ControlledFileApplicationNodeRecord[]
  relationEvidence?: RelationRoundEvidence | null
  controlledFileId: Identity
  versionNo: string
  applicationType: DccApplicationType
  bpmRound: string
  attributeRound: number | null
  recorded: boolean
  unavailableReason: 'NOT_RECORDED' | 'NOT_FROZEN' | null
  defaultSource: ProjectAttributes | null
  actualAttributes: ProjectAttributes | null
  signatures: Array<{
    id?: Identity
    taskId?: string | null
    processInstanceId: string
    actorNicknameSnapshot?: string | null
    actionType?: string | null
    actorDeptNameSnapshot?: string | null
    signedAt?: string | number | readonly number[] | null
  }>
}
interface SelectorRow {
  tenantId: Identity
  id: Identity
  masterId: Identity
  latestControlledFileId: Identity
  currentActiveControlledFileId: Identity
  dccProjectCodeId: Identity
  projectName: string
  projectFolderId?: Identity | null
  projectFolderName?: string | null
  sourceOriginalFileName: string
  fileNumber: string
  versionNo: string
  status: string
  controlled: boolean
  pendingEffect: boolean
  executable: boolean
  canPreview: boolean
}
const identity = (value: Identity): string => {
  const text = String(value)
  if (
    (typeof value === 'number' && !Number.isSafeInteger(value)) ||
    !/^[1-9][0-9]*$/.test(text) ||
    BigInt(text) > 9223372036854775807n
  )
    throw new Error('文件或项目身份无效')
  return text
}
export interface ControlledFileApplicationNodeRecord {
  recordId: string
  controlledFileId: Identity
  bpmRound: string
  taskId: string
  stageCode: string
  nodeName?: string | null
  actionType: string
  result?: string | null
  actorId?: Identity | null
  actorUsernameSnapshot?: string | null
  actorNicknameSnapshot?: string | null
  actorDeptNameSnapshot?: string | null
  comment?: string | null
  occurredAt?: string | number | readonly number[] | null
  signatureId?: Identity | null
  signatureEvidenceStatus?: string | null
  assignmentTarget?: { userId: Identity; nickname?: string | null; obligationDepartmentName?: string | null } | null
  confirmedFields: Array<{ field: string; label: string; value: string | null }>
}

/** Validates supplied read facts; it does not establish backend model or permission provenance. */
export const validateControlledFileRoundFlowContract = (
  row: ControlledFileApplicationRound
): ControlledFileRoundFlowContract | null | undefined => {
  const flow = row.flowContract
  if (flow == null) return flow
  const fail = (): never => { throw new Error('申请轮次流程合同身份、规则或缺记录状态不一致') }
  const exactId = (value: unknown): string => {
    if ((typeof value !== 'string' && typeof value !== 'number')
      || (typeof value === 'number' && !Number.isSafeInteger(value))
      || !/^[1-9][0-9]*$/.test(String(value)) || BigInt(String(value)) > 9223372036854775807n) fail()
    return String(value)
  }
  const exactText = (value: unknown): value is string =>
    typeof value === 'string' && value.length > 0 && value === value.trim()
  if (typeof flow !== 'object' || Array.isArray(flow)) fail()
  exactId(flow.tenantId)
  if (exactId(flow.controlledFileId) !== identity(row.controlledFileId)
    || flow.applicationType !== row.applicationType || flow.bpmRound !== row.bpmRound
    || flow.attributeRound !== row.attributeRound
    || !exactText(flow.processDefinitionId) || !exactText(flow.deploymentId)
    || typeof flow.definitionDigest !== 'string' || !/^[a-f0-9]{64}$/.test(flow.definitionDigest)
    || flow.processDefinitionKey !== ({
      UPLOAD: 'dcc-controlled-file-upload', REVISION: 'dcc-controlled-file-revision',
      OBSOLETE: 'dcc-controlled-file-obsolete'
    } as const)[row.applicationType]) fail()
  const obsolete = row.applicationType === 'OBSOLETE'
  if (obsolete) {
    exactId(flow.formInstanceId)
    if (!exactText(flow.formInstanceCode) || flow.needTraining !== null) fail()
  } else if (flow.formInstanceId !== null || flow.formInstanceCode !== null) fail()
  if (flow.status === 'READY') {
    if (flow.unavailableReason !== null) fail()
    if (obsolete) {
      if (flow.executionContract !== 'PUBLIC_MATRIX_NATIVE_V1'
        || flow.flowRule !== 'PUBLIC_V5_OBSOLETE_APPROVAL_END') fail()
    } else {
      if (typeof flow.needTraining !== 'boolean') fail()
      const supported = (flow.executionContract === 'PUBLIC_MATRIX_NATIVE_V1'
        && flow.flowRule === 'PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL')
        || (flow.executionContract === 'PUBLIC_MATRIX_NATIVE_V2'
          && flow.flowRule === 'PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM')
        || (flow.executionContract === 'LEGACY_V4_ACTION'
          && flow.flowRule === 'LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL')
      if (!supported) fail()
    }
  } else {
    if (flow.executionContract !== null || flow.flowRule !== null
      || (flow.needTraining !== null && typeof flow.needTraining !== 'boolean')) fail()
    if (flow.status === 'NOT_RECORDED') {
      if (flow.unavailableReason !== (obsolete
        ? 'FORM_PREPARATION_NOT_RECORDED' : 'HISTORICAL_CONTRACT_NOT_RECORDED')) fail()
    } else if (flow.status !== 'UNSUPPORTED_DEFINITION'
      || flow.unavailableReason !== 'UNSUPPORTED_HISTORICAL_GRAPH') fail()
  }
  return flow
}

type WorkingApplicationResponse = Omit<WorkingApplicationAttributes, 'effectiveDate'> & {
  effectiveDate: string | [number, number, number] | null
}
const applicationDate = (value: WorkingApplicationResponse['effectiveDate']): string | null => {
  if (value === null) return null
  const date =
    Array.isArray(value) && value.length === 3 && value.every(Number.isInteger)
      ? `${String(value[0]).padStart(4, '0')}-${String(value[1]).padStart(2, '0')}-${String(value[2]).padStart(2, '0')}`
      : value
  if (typeof date !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(date))
    throw new Error('草稿生效日期格式无效')
  const [year, month, day] = date.split('-').map(Number)
  const calendar = new Date(0)
  calendar.setUTCFullYear(year, month - 1, day)
  if (
    year < 1 ||
    calendar.getUTCFullYear() !== year ||
    calendar.getUTCMonth() !== month - 1 ||
    calendar.getUTCDate() !== day
  )
    throw new Error('草稿生效日期不是有效日历日期')
  return date
}
const validateWorkingAttributes = (
  value: WorkingApplicationResponse
): WorkingApplicationAttributes => {
  if (
    !value ||
    !['UPLOAD', 'REVISION'].includes(value.applicationType) ||
    (value.effectiveMode !== null && !['AFTER_STANDARDIZATION_SIGNATURE','DELAYED','PRESET_DATE'].includes(value.effectiveMode)) ||
    (value.needTraining !== null && typeof value.needTraining !== 'boolean') ||
    typeof value.canSubmit !== 'boolean' ||
    (value.selectedSignoffDepartmentIds !== null &&
      !Array.isArray(value.selectedSignoffDepartmentIds)) ||
    (value.selectedDistributionDepartmentIds !== null && !Array.isArray(value.selectedDistributionDepartmentIds))
  )
    throw new Error('草稿申请准备信息不完整')
  identity(value.controlledFileId)
  identity(value.projectId)
  validateAttributes(value.defaultSource)
  validateAttributes(value.actual)
  value.selectedSignoffDepartmentIds?.forEach(identity)
  value.selectedDistributionDepartmentIds?.forEach(identity)
  if (value.draftApprovalMatrix != null) validateDraftApprovalMatrix(value.draftApprovalMatrix)
  if (value.newActionBaselineRequired !== undefined || value.newActionBaselinePreparationAllowed !== undefined) {
    if (typeof value.newActionBaselineRequired !== 'boolean' ||
        typeof value.newActionBaselinePreparationAllowed !== 'boolean' ||
        (value.newActionBaselinePreparationAllowed && !value.newActionBaselineRequired) ||
        (value.newActionBaselineRequired && (value.applicationType !== 'REVISION' ||
          value.draftApprovalMatrix != null || value.canSubmit)) ||
        (value.newActionBaselinePreparationAllowed && (!['PUBLIC_MATRIX_NATIVE_V1', 'PUBLIC_MATRIX_NATIVE_V2'].includes(value.executionContract ?? '') ||
          typeof value.preparedProcessDefinitionId !== 'string' || !value.preparedProcessDefinitionId.trim() ||
          value.preparedProcessDefinitionId !== value.preparedProcessDefinitionId.trim() ||
          typeof value.preparedDefinitionDigest !== 'string' || !/^[a-f0-9]{64}$/.test(value.preparedDefinitionDigest)))) {
      throw new Error('新升版审批配置准备资格事实不完整或矛盾')
    }
  }
  if (value.approvalSourceMode === 'APPLICATION_CUSTOM') {
    if (value.draftApprovalMatrix != null || !value.applicationContextKey?.trim() || !Number.isInteger(value.attributeRound)
      || (value.attributeRound ?? 0) < 1 || typeof value.applicationCustomMatrixDigest !== 'string' || !/^[a-f0-9]{64}$/.test(value.applicationCustomMatrixDigest))
      throw new Error('已保存自定义申请的来源、上下文、轮次或配置摘要不完整')
    const custom = value.applicationCustomMatrix
    if (custom !== null) {
      if (!custom || Object.keys(custom).length !== 2 || !Object.hasOwn(custom, 'approverUserId') || !Object.hasOwn(custom, 'documentControlUserId')) throw new Error('已保存自定义申请的实际账号配置缺失')
      if (custom.approverUserId !== null) identity(custom.approverUserId)
      if (custom.documentControlUserId !== null) identity(custom.documentControlUserId)
    }
  } else if (value.approvalSourceMode != null && value.approvalSourceMode !== 'PUBLIC_MATRIX') throw new Error('申请保存的审批来源无效')
  return { ...value, effectiveDate: applicationDate(value.effectiveDate) }
}
export interface RelationRoundEvidence {
  tenantId: string
  fileId: string
  masterId: string
  projectId: string
  applicationType: DccApplicationType
  attributeRound: number
  bpmRound: string
  recorded: boolean
  unavailableReason: string | null
  relationDigest: string
  originalUploadSelections: RelationStageSelection[]
  completeSelections: RelationStageSelection[]
  stages: Array<{
    signatureEvidenceStatus: string
    signatureIdentityMatches: boolean
    snapshot: {
      header: {
        id: string; tenantId: string; sourceFileId: string; sourceMasterId: string; projectId: string
        processInstanceId: string; taskId: string; obligationId: string; stageCode: string; nodeInstanceId: string
        departmentId: string | null; actorId: string; signatureId: string; signedAt: string
        commandKey: string; payloadHash: string; expectedDigest: string; completeDigest: string
        actionType: string; reason: string; applicationType: DccApplicationType | null
        attributeRound: number | null; formActionInstanceId: string | null
      }
      members: Array<{
        tenantId: string; snapshotId: string; relatedMasterId: string; selectedControlledFileId: string
        projectId: string; fileNumberSnapshot: string; fileNameSnapshot: string; versionNoSnapshot: string
        origin: string; supplemental: boolean; assigneeUserId: string | null; dueAt: string | null
      }>
    }
  }>
}
export interface RelationStageSelection {
  relatedMasterId: string; selectedControlledFileId: string; projectId: string
  fileNumber: string; fileName: string; versionNo: string; origin: string; supplemental: boolean
}

const validateDraftApprovalMatrix = (matrix: ApprovalMatrixBaselineVO): void => {
  if (!matrix || matrix.formatVersion !== 'DCC_APPROVAL_MATRIX_BASELINE_V2'
    || !Number.isSafeInteger(matrix.routeVersionNo) || matrix.routeVersionNo <= 0
    || typeof matrix.matrixDigest !== 'string' || !/^[a-f0-9]{64}$/.test(matrix.matrixDigest)
    || !Array.isArray(matrix.nodes) || !matrix.nodes.length) throw new Error('草稿原审批矩阵身份或配置事实不完整')
  for (const id of [matrix.tenantId, matrix.categoryId, matrix.fileTypeTaxonomyId, matrix.routeId]) {
    if (typeof id !== 'string') throw new Error('草稿原矩阵ID不是精确十进制字符串')
    identity(id)
  }
  const seen = new Set<string>()
  for (const node of matrix.nodes) {
    if (!node || typeof node.id !== 'string' || seen.has(identity(node.id)))
      throw new Error('草稿原矩阵规则身份缺失或重复')
    seen.add(node.id)
    if (node.candidateSourceId !== null) identity(node.candidateSourceId)
    if (node.subjectId !== null) identity(node.subjectId)
    if (node.candidateSourceIds !== null && typeof node.candidateSourceIds !== 'string')
      throw new Error('草稿原矩阵候选配置不是正式原始CSV')
  }
}
export const getWorkingApplicationAttributes = async (
  fileId: Identity
): Promise<WorkingApplicationAttributes> => {
  const id = identity(fileId)
  const value = validateWorkingAttributes(
    await request.get({ url: `/dcc/controlled-files/${id}/working-attributes` })
  )
  if (identity(value.controlledFileId) !== id) throw new Error('草稿申请响应与所选文件不一致')
  return value
}

export const restoreWorkingApplicationAttributes = async (
  fileId: Identity
): Promise<WorkingApplicationAttributes> => {
  const id = identity(fileId)
  const saved = await request.post({
    url: `/dcc/controlled-files/${id}/working-attributes/restore`,
    ignoreErrorMessage: true
  })
  if (saved !== true) throw new Error('恢复原始默认属性未确认成功，请刷新核对')
  return getWorkingApplicationAttributes(id)
}

export const getSignoffAssignmentContext = async (
  fileId: Identity,
  taskId: string
): Promise<SignoffAssignmentContext> => {
  const id = identity(fileId)
  if (!taskId || taskId !== taskId.trim()) throw new Error('请指定真实会签任务')
  const result = await request.get<SignoffAssignmentContext>({
    url: `/dcc/controlled-file/workflow-lifecycle/${id}/signoff-assignment-context`,
    params: { taskId }
  })
  if (
    !result ||
    identity(result.controlledFileId) !== id ||
    result.taskId !== taskId ||
    !result.processInstanceId ||
    !['dcc-controlled-file-upload', 'dcc-controlled-file-revision', 'dcc-controlled-file-obsolete'].includes(result.processDefinitionKey) ||
    !result.obligationId ||
    typeof result.assigned !== 'boolean' ||
    typeof result.canAssign !== 'boolean' ||
    !Array.isArray(result.assigneeOptions)
  )
    throw new Error('会签指派响应身份或资格事实缺失')
  identity(result.departmentId)
  result.assigneeOptions.forEach((user) => {
    identity(user.id)
    if (!user.name) throw new Error('会签候选人名称缺失')
  })
  return result
}

export const getControlledFileRevisionOptions = async (
  baselineId: Identity
): Promise<ControlledFileRevisionOptions> => {
  const id = identity(baselineId)
  const result = await request.get<ControlledFileRevisionOptions>({
    url: `/dcc/controlled-files/${id}/revision-options`
  })
  if (!result || identity(result.controlledBaselineId) !== id || !Array.isArray(result.iterations)
      || result.iterations.some(row => !row || (row.createTime !== null && typeof row.createTime !== 'string' && typeof row.createTime !== 'number')
        || (typeof row.createTime === 'number' && !Number.isSafeInteger(row.createTime))
        || (row.changeDescription !== null && typeof row.changeDescription !== 'string')))
    throw new Error('升版准备响应与所选受控版本不一致')
  return result
}

export const saveWorkingApplicationAttributes = async (
  fileId: Identity,
  actual: ProjectAttributes,
  selection: WorkingApplicationSelection
): Promise<boolean> => {
  const id = identity(fileId)
  if (selection?.prepareNewActionBaseline != null && typeof selection.prepareNewActionBaseline !== 'boolean')
    throw new Error('请明确是否准备本次新升版审批配置')
  if (selection?.prepareNewActionBaseline === true && (
    !['PUBLIC_MATRIX_NATIVE_V1', 'PUBLIC_MATRIX_NATIVE_V2'].includes(selection.executionContract ?? '') ||
    typeof selection.approvalMatrixBaselineToken !== 'string' || !selection.approvalMatrixBaselineToken.trim() ||
    typeof selection.applicationContextKey !== 'string' || !selection.applicationContextKey.trim() ||
    typeof selection.expectedProcessDefinitionId !== 'string' || !selection.expectedProcessDefinitionId.trim() ||
    typeof selection.expectedDefinitionDigest !== 'string' || !/^[a-f0-9]{64}$/.test(selection.expectedDefinitionDigest) ||
    typeof selection.expectedApprovalMatrixDigest !== 'string' || !/^[a-f0-9]{64}$/.test(selection.expectedApprovalMatrixDigest) ||
    typeof selection.expectedApprovalRouteId !== 'string' ||
    !Number.isSafeInteger(selection.expectedApprovalRouteVersionNo) || (selection.expectedApprovalRouteVersionNo ?? 0) <= 0))
    throw new Error('本次新升版审批配置准备缺少正式身份或预览凭证')
  if (selection?.prepareNewActionBaseline === true) identity(selection.expectedApprovalRouteId!)
  if (!selection || (selection.effectiveMode !== null &&
      !['AFTER_STANDARDIZATION_SIGNATURE','DELAYED','PRESET_DATE'].includes(selection.effectiveMode)) ||
      (selection.needTraining !== null && typeof selection.needTraining !== 'boolean') ||
      (selection.selectedSignoffDepartmentIds !== null && !Array.isArray(selection.selectedSignoffDepartmentIds)) ||
      (selection.selectedDistributionDepartmentIds !== null && !Array.isArray(selection.selectedDistributionDepartmentIds))) {
    throw new Error('请明确本次工作申请选择，未填写内容保持未填写')
  }
  const effectiveDate = applicationDate(selection.effectiveDate)
  if ((selection.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE' || selection.effectiveMode === 'DELAYED') && effectiveDate !== null) {
    throw new Error('文控审核后立即生效或延迟生效在申请阶段不填写预设日期')
  }
  const signoff = selection.selectedSignoffDepartmentIds === null ? null : selection.selectedSignoffDepartmentIds.map(identity)
  const distribution = selection.selectedDistributionDepartmentIds === null ? null : selection.selectedDistributionDepartmentIds.map(identity)
  if ((signoff && new Set(signoff).size !== signoff.length) || (distribution && new Set(distribution).size !== distribution.length)) {
    throw new Error('本次申请部门选择不能重复')
  }
  const data = { actual: JSON.parse(JSON.stringify(validateAttributes(actual))),
    ...selection, effectiveDate, selectedSignoffDepartmentIds: signoff, selectedDistributionDepartmentIds: distribution }
  const saved = await request.post<boolean>({
    url: `/dcc/controlled-files/${id}/working-attributes`,
    data,
    ignoreErrorMessage: true
  })
  if (saved !== true) throw new Error('草稿属性保存未确认成功，请刷新后核对')
  return true
}

export const getControlledFileApplicationEvidence = async (
  fileId: Identity,
  applicationType: DccApplicationType,
  bpmRound: string
): Promise<ControlledFileApplicationEvidence> => {
  const id = identity(fileId)
  if (
    !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(applicationType) ||
    !bpmRound ||
    bpmRound !== bpmRound.trim()
  ) {
    throw new Error('请指定文件申请类型及真实流程轮次')
  }
  const result = await request.get<ControlledFileApplicationEvidence>({
    url: `/dcc/controlled-files/${id}/application-evidence`,
    params: { applicationType, bpmRound }
  })
  if (
    !result ||
    identity(result.controlledFileId) !== id ||
    result.applicationType !== applicationType ||
    result.bpmRound !== bpmRound ||
    typeof result.recorded !== 'boolean' ||
    !Array.isArray(result.signatures)
  ) {
    throw new Error('申请证据响应与所选文件或流程轮次不一致')
  }
  if (
    result.recorded
      ? !result.defaultSource ||
        !result.actualAttributes ||
        !Number.isInteger(result.attributeRound) ||
        (result.attributeRound || 0) < 1
      : !['NOT_RECORDED', 'NOT_FROZEN'].includes(result.unavailableReason || '') ||
        result.actualAttributes != null ||
        result.defaultSource != null
  ) {
    throw new Error('申请证据缺少正式冻结事实')
  }
  if (result.recorded) {
    validateAttributes(result.defaultSource!)
    validateAttributes(result.actualAttributes!)
  }
  if (result.signatures.some(signature => signature.processInstanceId !== bpmRound))
    throw new Error('申请签名证据属于其他流程轮次')
  if (result.relationEvidence != null) validateRelationRoundEvidence(result.relationEvidence, result)
  return result
}

const validateRelationRoundEvidence = (relations: RelationRoundEvidence, evidence: ControlledFileApplicationEvidence): void => {
  const exact = (id: unknown): string => {
    if (typeof id !== 'string') throw new Error('历史关联身份必须为精确十进制字符串')
    return identity(id)
  }
  if (!relations || exact(relations.fileId) !== identity(evidence.controlledFileId)
    || relations.applicationType !== evidence.applicationType || relations.bpmRound !== evidence.bpmRound
    || relations.attributeRound !== evidence.attributeRound || typeof relations.recorded !== 'boolean'
    || !/^[a-f0-9]{64}$/.test(relations.relationDigest) || !Array.isArray(relations.stages)
    || !Array.isArray(relations.originalUploadSelections) || !Array.isArray(relations.completeSelections))
    throw new Error('历史关联证据与所选文件或正式申请轮次不一致')
  const tenant = exact(relations.tenantId), master = exact(relations.masterId), project = exact(relations.projectId)
  for (const selection of [...relations.originalUploadSelections, ...relations.completeSelections]) {
    exact(selection.relatedMasterId); exact(selection.selectedControlledFileId)
    if (exact(selection.projectId) !== project || !selection.versionNo || typeof selection.supplemental !== 'boolean')
      throw new Error('历史关联成员与冻结项目或版本不一致')
  }
  const heads = new Set<string>()
  for (const stage of relations.stages) {
    const head = stage?.snapshot?.header
    if (!head || heads.has(exact(head.id)) || exact(head.tenantId) !== tenant
      || exact(head.sourceFileId) !== relations.fileId || exact(head.sourceMasterId) !== master
      || exact(head.projectId) !== project || head.processInstanceId !== relations.bpmRound
      || head.applicationType !== null && head.applicationType !== relations.applicationType
      || head.attributeRound !== null && head.attributeRound !== relations.attributeRound
      || !head.taskId || !head.obligationId || !head.nodeInstanceId || !head.stageCode || !head.signedAt
      || !stage.signatureEvidenceStatus || typeof stage.signatureIdentityMatches !== 'boolean'
      || !Array.isArray(stage.snapshot.members)) throw new Error('历史关联节点身份或独立签名状态不完整')
    heads.add(head.id); exact(head.actorId); exact(head.signatureId)
    if (head.departmentId !== null) exact(head.departmentId)
    if (head.formActionInstanceId !== null) exact(head.formActionInstanceId)
    const members = new Set<string>()
    for (const member of stage.snapshot.members) {
      if (exact(member.tenantId) !== tenant || exact(member.snapshotId) !== head.id
        || exact(member.projectId) !== project || members.has(exact(member.relatedMasterId))
        || typeof member.supplemental !== 'boolean') throw new Error('历史关联节点成员与原节点不一致或重复')
      members.add(member.relatedMasterId); exact(member.selectedControlledFileId)
      if (member.assigneeUserId !== null) exact(member.assigneeUserId)
    }
  }
}

export const getControlledFileReplacementAttributes = async (
  selectedIterationId: Identity,
  controlledBaselineId: Identity
): Promise<WorkingApplicationAttributes> => {
  const id = identity(selectedIterationId), baseline = identity(controlledBaselineId)
  const value = validateWorkingAttributes(await request.get({
    url: `/dcc/controlled-files/${id}/replacement-attributes`,
    params: { controlledBaselineId: baseline },
    ignoreErrorMessage: true
  }))
  if (identity(value.controlledFileId) !== id || value.applicationType !== 'REVISION')
    throw new Error('换版申请响应与所选工作正文不一致')
  return value
}

export const loadDccSelectorPage = async (
  tenantId: Identity,
  query: SelectorQuery
): Promise<SelectorPage> => {
  const tenant = identity(tenantId)
  const directory = query.folderId !== undefined
  if (
    (directory && !query.projectId) ||
    !Number.isInteger(query.pageNo) ||
    query.pageNo < 1 ||
    !Number.isInteger(query.pageSize) ||
    query.pageSize < 1 ||
    query.pageSize > 200
  )
    throw new Error('文件选择器查询范围或分页无效')
  const response = await request.get<{ list: SelectorRow[]; total: number }>({
    url: '/dcc/controlled-files/browser-page',
    params: {
      selectorScope: directory ? 'PROJECT_FOLDER' : 'GLOBAL',
      ...(directory
        ? {
            dccProjectCodeId: identity(query.projectId!),
            projectFolderId: identity(query.folderId!)
          }
        : query.projectId === undefined ? {} : { dccProjectCodeId: identity(query.projectId) }),
      keyword: query.keyword,
      pageNo: query.pageNo,
      pageSize: query.pageSize
    }
  })
  if (
    !response ||
    !Array.isArray(response.list) ||
    !Number.isSafeInteger(response.total) ||
    response.total < 0
  ) {
    throw new Error('文件选择器响应分页无效')
  }
  const masters = new Set<string>()
  const list: FileCandidate[] = response.list.map((row) => {
    const id = identity(row.id),
      masterId = identity(row.masterId),
      projectId = identity(row.dccProjectCodeId)
    if (
      identity(row.tenantId) !== tenant ||
      identity(row.currentActiveControlledFileId) !== id ||
      masters.has(masterId) ||
      row.controlled !== true ||
      row.status !== 'ACTIVE' ||
      typeof row.canPreview !== 'boolean' ||
      row.pendingEffect !== false ||
      row.executable !== true ||
      !row.projectName ||
      !row.sourceOriginalFileName ||
      !row.fileNumber ||
      !row.versionNo ||
      (row.projectFolderId != null) !== Boolean(row.projectFolderName) ||
      (query.projectId !== undefined && projectId !== identity(query.projectId)) ||
      (directory && (row.projectFolderId == null || identity(row.projectFolderId) !== identity(query.folderId!)))
    ) {
      throw new Error('文件选择器响应包含失配的版本、项目或权限事实')
    }
    identity(row.latestControlledFileId)
    masters.add(masterId)
    return {
      tenantId: tenant,
      controlledFileId: id,
      masterId,
      projectId,
      projectName: row.projectName,
      folderName: row.projectFolderName || '未记录',
      fileName: row.sourceOriginalFileName,
      fileNumber: row.fileNumber,
      versionNo: row.versionNo,
      status: row.status,
      controlled: row.controlled,
      pendingEffect: row.pendingEffect,
      executable: row.executable,
      canPreview: row.canPreview
    }
  })
  return { list, total: response.total }
}

export const getControlledFileApplicationRounds = async (
  fileId: Identity
): Promise<ControlledFileApplicationRound[]> => {
  const id = identity(fileId)
  const rows = await request.get<ControlledFileApplicationRound[]>({
    url: `/dcc/controlled-files/${id}/application-rounds`,
    ignoreErrorMessage: true
  })
  if (!Array.isArray(rows)) throw new Error('申请轮次响应缺失')
  const keys = new Set<string>()
  return rows.map((row) => {
    if (
      !row || identity(row.controlledFileId) !== id ||
      !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(row.applicationType) ||
      typeof row.bpmRound !== 'string' || !row.bpmRound || row.bpmRound !== row.bpmRound.trim() ||
      !Number.isSafeInteger(row.attributeRound) || row.attributeRound < 1
    ) throw new Error('申请轮次与所选文件不一致或映射缺失')
    const key = JSON.stringify([row.applicationType, row.bpmRound])
    if (keys.has(key)) throw new Error('申请轮次映射重复')
    keys.add(key)
    validateControlledFileRoundFlowContract(row)
    return { ...row, controlledFileId: id }
  })
}

export const getControlledFileRelationPermissions = async (
  fileId: Identity
): Promise<ControlledFileRelationPermissions> => {
  const id = identity(fileId)
  const result = await request.get<ControlledFileRelationPermissions>({
    url: `/dcc/controlled-files/${id}/relation-permissions`,
    ignoreErrorMessage: true
  })
  if (!result || identity(result.controlledFileId) !== id ||
      typeof result.hasCurrentControlledSource !== 'boolean' ||
      (result.hasCurrentControlledSource && result.currentActiveControlledFileId == null) ||
      typeof result.canEdit !== 'boolean' || typeof result.canPreview !== 'boolean' ||
      typeof result.projectName !== 'string' || !result.projectName.trim() ||
      typeof result.fileNumber !== 'string' || !result.fileNumber.trim() ||
      typeof result.fileName !== 'string' || !result.fileName ||
      typeof result.versionNo !== 'string' || !result.versionNo || typeof result.status !== 'string' || !result.status ||
      typeof result.controlled !== 'boolean' || typeof result.pendingEffect !== 'boolean' || typeof result.executable !== 'boolean' ||
      result.pendingEffect !== (result.status === 'CONTROLLED_PENDING_EFFECTIVE') ||
      (result.controlled && !['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(result.status)) ||
      (result.pendingEffect && (!result.controlled || result.executable)) ||
      (result.executable && (!result.controlled || result.status !== 'ACTIVE')) ||
      (result.projectFolderId != null) !== Boolean(result.projectFolderName))
    throw new Error('关联权限响应与所选文件不一致或权限事实缺失')
  return { ...result, controlledFileId: id, tenantId: identity(result.tenantId), masterId: identity(result.masterId),
    projectId: identity(result.projectId), projectFolderId: result.projectFolderId == null ? null : identity(result.projectFolderId),
    latestControlledFileId: result.latestControlledFileId == null ? null : identity(result.latestControlledFileId),
    currentActiveControlledFileId: result.currentActiveControlledFileId == null ? null : identity(result.currentActiveControlledFileId) }
}

interface ProjectBrowserRow extends Omit<SelectorRow, 'latestControlledFileId' | 'currentActiveControlledFileId'> {
  latestControlledFileId?: Identity | null
  currentActiveControlledFileId?: Identity | null
  fileName: string
}
export const DCC_PROJECT_BROWSER_STATUSES = [
  'DRAFT', 'WORKING', 'PENDING_DOC_CONTROL_REVIEW', 'PENDING_MATRIX_REVIEW',
  'PENDING_MATRIX_APPROVAL', 'PENDING_DOC_CONTROL_APPROVAL', 'PENDING_APPLICANT_REWORK',
  'PENDING_APPLICANT_TRAINING_RECORD', 'READY_TO_PUBLISH', 'FINALIZING', 'TRAINING_IN_PROGRESS',
  'PENDING_MANUAL_DISTRIBUTION', 'ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE', 'REJECTED',
  'WITHDRAWN', 'OBSOLETE', 'SUPERSEDED', 'FINALIZATION_FAILED', 'SUBMIT_FAILED',
  'APPROVING', 'APPROVED', 'STAMPING', 'STAMP_FAILED', 'STAMPED'
] as const
export type DccProjectBrowserStatus = (typeof DCC_PROJECT_BROWSER_STATUSES)[number]
export interface ProjectBrowserOptions { latestVersionOnly?: boolean; status?: DccProjectBrowserStatus }
const projectBrowserStatuses = new Set<string>(DCC_PROJECT_BROWSER_STATUSES)

export const loadDccProjectBrowserPage = async (
  tenantId: Identity,
  query: SelectorQuery,
  options: ProjectBrowserOptions = {}
): Promise<SelectorPage> => {
  const tenant = identity(tenantId)
  if (!options || (options.latestVersionOnly !== undefined && typeof options.latestVersionOnly !== 'boolean') ||
      (options.status !== undefined && (typeof options.status !== 'string' || !projectBrowserStatuses.has(options.status))))
    throw new Error('项目浏览版本选项或状态无效')
  const latestVersionOnly = options.latestVersionOnly ?? true
  const directory = query.folderId !== undefined
  if (
    (directory && !query.projectId) ||
    (!directory && query.projectId !== undefined) ||
    !Number.isInteger(query.pageNo) || query.pageNo < 1 ||
    !Number.isInteger(query.pageSize) || query.pageSize < 1 || query.pageSize > 200
  ) throw new Error('项目文件查询范围或分页无效')
  const projectId = directory ? identity(query.projectId!) : undefined
  const folderId = directory ? identity(query.folderId!) : undefined
  const response = await request.get<{ list: ProjectBrowserRow[]; total: number }>({
    url: '/dcc/controlled-files/browser-page',
    params: {
      browserScope: directory ? 'PROJECT_FOLDER' : 'GLOBAL',
      ...(directory ? { dccProjectCodeId: projectId, projectFolderId: folderId } : {}),
      latestVersionOnly,
      ...(options.status === undefined ? {} : { status: options.status }),
      keyword: query.keyword, pageNo: query.pageNo, pageSize: query.pageSize
    }
  })
  if (!response || !Array.isArray(response.list) || !Number.isSafeInteger(response.total) || response.total < 0)
    throw new Error('项目文件响应分页无效')
  const versions = new Set<string>()
  const list = response.list.map((row): FileCandidate => {
    const id = identity(row.id), masterId = identity(row.masterId), rowProjectId = identity(row.dccProjectCodeId)
    const rowFolderId = row.projectFolderId == null ? null : identity(row.projectFolderId)
    if (
      identity(row.tenantId) !== tenant || versions.has(id) ||
      typeof row.controlled !== 'boolean' || typeof row.pendingEffect !== 'boolean' ||
      typeof row.executable !== 'boolean' || typeof row.canPreview !== 'boolean' ||
      !row.status || !row.fileName || !row.fileNumber || !row.versionNo || !row.projectName ||
      (rowFolderId !== null) !== Boolean(row.projectFolderName) ||
      row.pendingEffect !== (row.status === 'CONTROLLED_PENDING_EFFECTIVE') ||
      (row.pendingEffect && (!row.controlled || row.executable)) ||
      (row.executable && (!row.controlled || row.status !== 'ACTIVE')) ||
      (options.status !== undefined && row.status !== options.status) ||
      (latestVersionOnly && (row.currentActiveControlledFileId == null || identity(row.currentActiveControlledFileId) !== id ||
        row.status !== 'ACTIVE' || row.controlled !== true || row.executable !== true || row.pendingEffect !== false)) ||
      (directory && (rowProjectId !== projectId || rowFolderId !== folderId))
    ) throw new Error('项目文件响应包含失配的版本、项目或权限事实')
    if (row.latestControlledFileId != null) identity(row.latestControlledFileId)
    if (row.currentActiveControlledFileId != null) identity(row.currentActiveControlledFileId)
    versions.add(id)
    return {
      tenantId: tenant, controlledFileId: id, masterId, projectId: rowProjectId,
      projectName: row.projectName, projectFolderId: rowFolderId,
      folderName: row.projectFolderName || '未记录',
      fileName: row.fileName, fileNumber: row.fileNumber, versionNo: row.versionNo,
      status: row.status, controlled: row.controlled, pendingEffect: row.pendingEffect,
      executable: row.executable, canPreview: row.canPreview
    }
  })
  return { list, total: response.total }
}
