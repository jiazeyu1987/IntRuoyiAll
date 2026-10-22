import type { ApprovalMatrixBaselineVO, ApprovalMatrixConfirmationSelection,
  ApplicationCustomMatrix, ControlledFileRoutePreviewReqVO, ControlledFileRouteReadinessVO } from '@/api/dcc/controlledFile/workflow'

export interface ApprovalMatrixScope {
  approvalSourceMode?: 'PUBLIC_MATRIX' | 'APPLICATION_CUSTOM'
  requesterId?: string; attributeRound?: number
  tenantId: string; projectId: string; categoryId: string; taxonomyId: string
  actionType: 'NEW' | 'REVISION' | 'OBSOLETE'; applicationContextKey: string; controlledFileId?: string
}
export interface MatrixSummaryRow { label: string; value: string }
export interface NewActionBaselineEligibility {
  controlledFileId: string; projectId: string; applicationType: string
  draftApprovalMatrix?: ApprovalMatrixBaselineVO | null
  canSubmit?: boolean
  executionContract?: string | null
  preparedProcessDefinitionId?: string | null
  preparedDefinitionDigest?: string | null
  newActionBaselineRequired?: boolean; newActionBaselinePreparationAllowed?: boolean
}
export const isNewActionBaselinePreparationAllowed = (facts: NewActionBaselineEligibility | undefined,
  scope: ApprovalMatrixScope): boolean => {
  if (!facts || scope.actionType !== 'REVISION' || !scope.controlledFileId || facts.applicationType !== 'REVISION'
    || facts.controlledFileId !== scope.controlledFileId || facts.projectId !== scope.projectId || facts.draftApprovalMatrix !== null
    || facts.canSubmit !== false || facts.newActionBaselineRequired !== true || facts.newActionBaselinePreparationAllowed !== true
    || !isPublicExecutionContract(facts.executionContract)) return false
  try { definitionIdentity(facts.preparedProcessDefinitionId); definitionDigest(facts.preparedDefinitionDigest); return true }
  catch { return false }
}
interface MatrixRequest { sequence: number; scope: string; request: ControlledFileRoutePreviewReqVO }
const copy = <T>(value: T): T => JSON.parse(JSON.stringify(value))
const identity = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value) || BigInt(value) > 9223372036854775807n)
    throw new Error('审批矩阵身份缺失或精度损失')
  return value
}
const tokenText = (value: unknown): string => {
  if (typeof value !== 'string' || !value.trim() || value !== value.trim())
    throw new Error('审批矩阵票据未配置或响应缺失，请重新读取正式预检')
  return value
}
const digest = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[a-f0-9]{64}$/.test(value)) throw new Error('审批矩阵摘要缺失或无效')
  return value
}
export const PUBLIC_APPLICATION_EXECUTION_CONTRACT = 'PUBLIC_MATRIX_NATIVE_V2' as const
type PublicExecutionContract = 'PUBLIC_MATRIX_NATIVE_V1' | 'PUBLIC_MATRIX_NATIVE_V2'
const isPublicExecutionContract = (value: unknown): value is PublicExecutionContract =>
  value === 'PUBLIC_MATRIX_NATIVE_V1' || value === 'PUBLIC_MATRIX_NATIVE_V2'
interface PreparedDefinition { executionContract: PublicExecutionContract; processDefinitionId: string; definitionDigest: string }
const definitionIdentity = (value: unknown): string => {
  if (typeof value !== 'string' || !value || value !== value.trim() || /[\x00-\x1f\x7f]/.test(value))
    throw new Error('本次流程定义准备身份缺失或无效，请读取正式流程准备')
  return value
}
const definitionDigest = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[a-f0-9]{64}$/.test(value)) throw new Error('本次流程定义准备摘要缺失或无效')
  return value
}
const readPreparedDefinition = (response: ControlledFileRouteReadinessVO): PreparedDefinition | undefined => {
  if (response.executionContract != null && !isPublicExecutionContract(response.executionContract))
    throw new Error('新申请流程执行合同必须为正式公共矩阵合同')
  const id = response.preparedProcessDefinitionId, hash = response.preparedDefinitionDigest
  if (id == null && hash == null) return undefined
  if (!isPublicExecutionContract(response.executionContract)) throw new Error('本次流程准备缺少明确的公共执行合同')
  return { executionContract: response.executionContract, processDefinitionId: definitionIdentity(id), definitionDigest: definitionDigest(hash) }
}
const rawKeys = ['id', 'stageNo', 'stageCode', 'stageName', 'stageOrder', 'sort', 'candidateSourceType',
  'candidateSourceId', 'candidateSourceIds', 'approveMethod', 'approveRatio', 'requireAllApprovals',
  'required', 'stageType', 'subjectLabel', 'marker', 'subjectType', 'subjectId', 'subjectName', 'subjectDepartmentPath', 'ruleRemark']

/** Validates transport facts only. The server canonicalizer/HMAC is authoritative; no client baseline is generated. */
export const validateApprovalMatrixBaseline = (matrix: ApprovalMatrixBaselineVO, scope: ApprovalMatrixScope) => {
  if (!matrix || matrix.formatVersion !== 'DCC_APPROVAL_MATRIX_BASELINE_V2' || matrix.actionType !== 'LEGACY'
    || matrix.tenantId !== scope.tenantId || matrix.categoryId !== scope.categoryId || matrix.fileTypeTaxonomyId !== scope.taxonomyId
    || !Number.isSafeInteger(matrix.routeVersionNo) || matrix.routeVersionNo < 1
    || typeof matrix.effectiveTime !== 'string' || !matrix.effectiveTime
    || !Object.prototype.hasOwnProperty.call(matrix, 'routeRemark') || !Array.isArray(matrix.nodes) || !matrix.nodes.length)
    throw new Error('审批矩阵正式来源或原始配置事实不完整')
  ;[matrix.tenantId, matrix.categoryId, matrix.fileTypeTaxonomyId, matrix.routeId].forEach(identity)
  digest(matrix.matrixDigest)
  const ids = new Set<string>()
  matrix.nodes.forEach(node => {
    if (!node || rawKeys.some(key => !Object.prototype.hasOwnProperty.call(node, key)
      || node[key as keyof typeof node] === undefined) || ids.has(identity(node.id)))
      throw new Error('审批矩阵原始规则字段缺失或身份重复')
    ids.add(node.id)
    if (node.candidateSourceId !== null) identity(node.candidateSourceId)
    if (node.subjectId !== null) identity(node.subjectId)
    if (node.candidateSourceIds !== null && typeof node.candidateSourceIds !== 'string') throw new Error('审批矩阵原候选CSV事实缺失')
  })
}

export interface ApprovalMatrixSelectionIntent { phase: 'SUBMIT' | 'SAVE'; actionType: ApprovalMatrixScope['actionType'] }
export interface ScopedApprovalMatrixSelection extends ApprovalMatrixConfirmationSelection {
  selectedSignoffDepartmentIds?: string[] | null
}
const customValue = (value: unknown): ApplicationCustomMatrix | null => {
  if (value === null) return null
  if (!value || typeof value !== 'object' || Array.isArray(value)
    || JSON.stringify(Object.keys(value).sort()) !== JSON.stringify(['approverUserId', 'documentControlUserId']))
    throw new Error('本次自定义配置字段缺失或无效')
  const result = value as ApplicationCustomMatrix
  // The formal Long serializer sends safe IDs as numbers and large IDs as strings.
  // Canonical application data always uses strings; never stringify an unsafe number.
  const actorId = (id: unknown): string | null => id === null ? null
    : identity(typeof id === 'number' && Number.isSafeInteger(id) ? String(id) : id)
  return { approverUserId: actorId(result.approverUserId), documentControlUserId: actorId(result.documentControlUserId) }
}
const customDepartments = (value: unknown): string[] | null => {
  if (value === null) return null
  if (!Array.isArray(value)) throw new Error('本次自定义会签部门保存值缺失')
  const result = value.map(id => identity(typeof id === 'number' && Number.isSafeInteger(id) ? String(id) : id))
  if (new Set(result).size !== result.length) throw new Error('会签部门不能重复')
  return result.sort((a, b) => BigInt(a) < BigInt(b) ? -1 : BigInt(a) > BigInt(b) ? 1 : 0)
}
const sameCustom = (first: unknown, second: unknown): boolean => JSON.stringify(customValue(first)) === JSON.stringify(customValue(second))
interface SupervisorSource { digest: string; token: string }
const supervisorSourcePair = (value: unknown, token: unknown, required: boolean): SupervisorSource | undefined => {
  if (value == null && token == null) {
    if (required) throw new Error('直属主管正式来源未准备，请重新读取正式预检')
    return undefined
  }
  if (typeof value !== 'string' || !/^[a-f0-9]{64}$/.test(value) || typeof token !== 'string'
    || !token || token !== token.trim() || /[\s\x00-\x1f\x7f]/.test(token))
    throw new Error('直属主管来源摘要或票据缺失、半组或无效，请重新读取正式预检')
  return { digest: value, token }
}
export const requireApprovalMatrixSelection = (selection: ApprovalMatrixConfirmationSelection | undefined,
  intent: ApprovalMatrixSelectionIntent = { phase: 'SUBMIT', actionType: 'NEW' }, actual?: {
    applicationCustomMatrix: ApplicationCustomMatrix | null; selectedSignoffDepartmentIds: Array<number | string> | null
  }): ScopedApprovalMatrixSelection => {
  if (selection?.approvalSourceMode === 'APPLICATION_CUSTOM') {
    if (!isPublicExecutionContract(selection.executionContract)) throw new Error('本次自定义申请流程合同未准备')
    definitionIdentity(selection.expectedProcessDefinitionId); definitionDigest(selection.expectedDefinitionDigest)
    digest(selection.expectedApplicationCustomMatrixDigest); tokenText(selection.applicationCustomMatrixToken)
    if (!['PREVIEW', 'RESERVED'].includes(selection.applicationCustomMatrixPhase || '') || !selection.applicationContextKey
      || selection.applicationContextKey !== selection.applicationContextKey.trim() || selection.applicationContextKey.length > 128
      || /[\x00-\x1f\x7f]/.test(selection.applicationContextKey)) throw new Error('本次自定义申请阶段或上下文缺失')
    if ([selection.expectedApprovalRouteId, selection.expectedApprovalRouteVersionNo, selection.expectedApprovalMatrixDigest,
      selection.approvalMatrixBaselineToken, selection.approvalMatrixConfirmationToken, selection.matrixChangeConfirmed].some(value => value != null))
      throw new Error('本次自定义来源不能借用公共矩阵身份')
    const value = customValue(selection.applicationCustomMatrix)
    const departments = customDepartments((selection as ScopedApprovalMatrixSelection).selectedSignoffDepartmentIds)
    if (intent.phase === 'SUBMIT' && !departments?.length) throw new Error('请至少选择一个会签部门')
    if (actual && (!sameCustom(value, actual.applicationCustomMatrix)
      || JSON.stringify(departments) !== JSON.stringify(customDepartments(actual.selectedSignoffDepartmentIds))))
      throw new Error('本次自定义账号或会签部门已变化，请重新读取正式预检')
    if (intent.phase === 'SUBMIT' && (!value?.approverUserId || !value.documentControlUserId)) throw new Error('请选择本次批准人和文控审核人')
    supervisorSourcePair(selection.expectedSupervisorSourceDigest, selection.supervisorSourceToken, intent.phase === 'SUBMIT')
    return { ...copy(selection), applicationCustomMatrix: value }
  }
  if (!selection || !selection.expectedApprovalRouteId || !Number.isSafeInteger(selection.expectedApprovalRouteVersionNo)
    || (selection.expectedApprovalRouteVersionNo ?? 0) < 1 || typeof selection.applicationContextKey !== 'string'
    || !selection.applicationContextKey || selection.applicationContextKey.length > 128
    || typeof selection.matrixChangeConfirmed !== 'boolean') throw new Error('请先读取并核对本次正式审批矩阵')
  if (!isPublicExecutionContract(selection.executionContract))
    throw new Error('新申请流程执行合同尚未明确准备，请重新读取正式预检')
  definitionIdentity(selection.expectedProcessDefinitionId); definitionDigest(selection.expectedDefinitionDigest)
  identity(selection.expectedApprovalRouteId); digest(selection.expectedApprovalMatrixDigest)
  tokenText(selection.approvalMatrixBaselineToken)
  if (selection.matrixChangeConfirmed) tokenText(selection.approvalMatrixConfirmationToken)
  else if (selection.approvalMatrixConfirmationToken != null) throw new Error('审批矩阵变化尚未明确确认')
  if (intent.actionType === 'NEW' || intent.actionType === 'REVISION')
    supervisorSourcePair(selection.expectedSupervisorSourceDigest, selection.supervisorSourceToken, intent.phase === 'SUBMIT')
  return copy(selection)
}

export class UploadApprovalMatrixState {
  private customInput: ApplicationCustomMatrix | null = null
  private customAccepted: ScopedApprovalMatrixSelection | undefined
  private scope!: ApprovalMatrixScope
  private sequence = 0
  private confirmationIdentity = ''
  private baselineToken: string | undefined
  private confirmationToken: string | null = null
  private changed = false
  private pending = false
  private prepared: PreparedDefinition | undefined
  private lastPrepared: PreparedDefinition | undefined
  private previousPrepared: PreparedDefinition | undefined
  private preparationError = ''
  private definitionNeedsConfirmation = false
  private newActionPreparation = false
  private supervisorSource: SupervisorSource | undefined
  private lastSupervisorSource: SupervisorSource | undefined
  private supervisorNeedsConfirmation = false
  private runtimeReady = false
  private runtimeError = ''
  before: ApprovalMatrixBaselineVO | undefined
  after: ApprovalMatrixBaselineVO | undefined
  constructor(scope: ApprovalMatrixScope, saved?: ApprovalMatrixBaselineVO | null, savedExecution?: NewActionBaselineEligibility) {
    this.reset(scope, saved)
    if (savedExecution) {
      if (savedExecution.controlledFileId !== scope.controlledFileId || savedExecution.projectId !== scope.projectId
        || !isPublicExecutionContract(savedExecution.executionContract))
        throw new Error('已保存正文的流程准备身份与当前申请不一致')
      this.lastPrepared = { executionContract: savedExecution.executionContract, processDefinitionId: definitionIdentity(savedExecution.preparedProcessDefinitionId),
        definitionDigest: definitionDigest(savedExecution.preparedDefinitionDigest) }
    }
  }
  reset(scope: ApprovalMatrixScope, saved?: ApprovalMatrixBaselineVO | null) {
    ;[scope.tenantId, scope.projectId, scope.categoryId, scope.taxonomyId].forEach(identity)
    if (scope.controlledFileId !== undefined) identity(scope.controlledFileId)
    if (!['NEW', 'REVISION', 'OBSOLETE'].includes(scope.actionType) || !scope.applicationContextKey
      || scope.applicationContextKey !== scope.applicationContextKey.trim() || scope.applicationContextKey.length > 128)
      throw new Error('审批矩阵申请上下文缺失或无效')
    this.scope = copy(scope); this.sequence++; this.before = undefined; this.after = undefined
    this.baselineToken = undefined; this.confirmationToken = null; this.confirmationIdentity = ''; this.changed = false
    this.pending = false
    this.prepared = undefined; this.lastPrepared = undefined; this.previousPrepared = undefined; this.preparationError = ''; this.definitionNeedsConfirmation = false
    this.newActionPreparation = false
    this.supervisorSource = undefined; this.lastSupervisorSource = undefined; this.supervisorNeedsConfirmation = false
    this.runtimeReady = false; this.runtimeError = ''
    this.customInput = null; this.customAccepted = undefined
    if (saved != null) { validateApprovalMatrixBaseline(saved, scope); this.before = copy(saved) }
  }
  scopeKey() { return JSON.stringify(this.scope) }
  setApplicationCustomMatrix(value: ApplicationCustomMatrix | null) {
    value = customValue(value)
    if (JSON.stringify(this.customInput) === JSON.stringify(value)) return
    this.customInput = copy(value); this.customAccepted = undefined; this.sequence++; this.pending = true
  }
  createDefaultPreview(): UploadApprovalMatrixState {
    const state = new UploadApprovalMatrixState(this.scope, this.before)
    state.baselineToken = this.baselineToken
    state.lastPrepared = this.lastPrepared ? copy(this.lastPrepared) : undefined
    return state
  }
  begin(selected?: Array<number | string>): MatrixRequest {
    this.sequence++
    this.pending = true
    return { sequence: this.sequence, scope: this.scopeKey(), request: {
      categoryId: this.scope.categoryId, dccProjectCodeId: this.scope.projectId, controlledFileId: this.scope.controlledFileId,
      actionType: this.scope.actionType, applicationContextKey: this.scope.applicationContextKey,
      executionContract: this.lastPrepared?.executionContract ?? PUBLIC_APPLICATION_EXECUTION_CONTRACT,
      expectedProcessDefinitionId: this.lastPrepared?.processDefinitionId, expectedDefinitionDigest: this.lastPrepared?.definitionDigest,
      approvalMatrixBaselineToken: this.baselineToken,
      ...(this.scope.approvalSourceMode === 'APPLICATION_CUSTOM' ? { applicationCustomMatrix: copy(this.customInput) } : {}),
      ...(this.newActionPreparation ? { prepareNewActionBaseline: true } : {}),
      ...(selected === undefined ? {} : { selectedSignoffDepartmentIds: [...selected] })
    } }
  }
  accept(request: MatrixRequest, response: ControlledFileRouteReadinessVO): boolean {
    if (request.sequence !== this.sequence || request.scope !== this.scopeKey()) return false
    if (this.scope.approvalSourceMode === 'APPLICATION_CUSTOM') {
      if (response.approvalSourceMode !== 'APPLICATION_CUSTOM' || response.derivationVersion !== 'DCC_APPLICATION_CUSTOM_MATRIX_V1'
        || response.routeId != null || response.routeVersionNo != null || response.approvalMatrix != null || response.matrixBefore != null || response.matrixAfter != null)
        throw new Error('本次自定义来源不能接收公共矩阵身份或错误来源')
      if (!sameCustom(request.request.applicationCustomMatrix, response.applicationCustomMatrix)) throw new Error('本次自定义配置与正式预检响应不一致')
      const phase = this.scope.controlledFileId ? 'RESERVED' : 'PREVIEW'
      if (response.applicationCustomMatrixPhase !== phase || response.applicationActionType !== this.scope.actionType)
        throw new Error('本次自定义预检阶段或申请类型不一致')
      const prepared = readPreparedDefinition(response)
      if (!prepared || prepared.executionContract !== request.request.executionContract
        || this.scope.controlledFileId && this.lastPrepared && (prepared.processDefinitionId !== this.lastPrepared.processDefinitionId
          || prepared.definitionDigest !== this.lastPrepared.definitionDigest))
        throw new Error('本次自定义申请的精确流程定义或执行合同不一致，不得隐式升级或替换')
      // The server authenticates canonical bytes and HMAC; the client compares their transport facts with the current request.
      let canonical: Record<string, any>
      try { canonical = JSON.parse(response.applicationCustomMatrixCanonical || '') }
      catch { throw new Error('本次自定义正式保存配置缺失或无法读取') }
      if (!canonical || JSON.stringify(Object.keys(canonical).sort()) !== JSON.stringify(['applicationCustomMatrix', 'approvalSourceMode', 'context', 'formatVersion', 'selectedSignoffDepartmentIds'])
        || canonical.approvalSourceMode !== 'APPLICATION_CUSTOM' || canonical.formatVersion !== (phase === 'PREVIEW' ? 'DCC_APPLICATION_CUSTOM_MATRIX_PREVIEW_V1' : 'DCC_APPLICATION_CUSTOM_MATRIX_V1')
        || !sameCustom(canonical.applicationCustomMatrix, response.applicationCustomMatrix)) throw new Error('本次自定义正式配置来源或阶段不一致')
      const context = canonical.context
      if (!context || context.tenantId !== this.scope.tenantId || context.projectId !== this.scope.projectId
        || context.categoryId !== this.scope.categoryId || context.taxonomyId !== this.scope.taxonomyId
        || context.applicationContextKey !== this.scope.applicationContextKey || context.applicationType !== (this.scope.actionType === 'NEW' ? 'UPLOAD' : this.scope.actionType)
        || context.processDefinitionId !== response.preparedProcessDefinitionId || context.definitionDigest !== response.preparedDefinitionDigest
        || this.scope.requesterId != null && context.requesterId !== this.scope.requesterId)
        throw new Error('本次自定义正式配置上下文与本次申请不一致')
      identity(context.requesterId)
      if (phase === 'PREVIEW' ? context.controlledFileId !== undefined || context.attributeRound !== undefined
        : context.controlledFileId !== this.scope.controlledFileId || !Number.isSafeInteger(context.attributeRound) || context.attributeRound < 1
          || this.scope.attributeRound != null && context.attributeRound !== this.scope.attributeRound)
        throw new Error('本次自定义已保留文件或轮次不一致')
      const departments = customDepartments(canonical.selectedSignoffDepartmentIds)
      if (JSON.stringify(departments) !== JSON.stringify(customDepartments(request.request.selectedSignoffDepartmentIds ?? null)))
        throw new Error('本次自定义正式配置会签部门与本次选择不一致')
      if (typeof response.ready !== 'boolean' || !Array.isArray(response.nodes) || !Array.isArray(response.blockers))
        throw new Error('本次自定义正式预检事实不完整')
      if (response.ready) {
        const signoff = response.nodes.filter(node => node.stageCode === 'MATRIX_REVIEW')
        if (signoff.length !== 1 || signoff[0].candidateSourceType !== 'DEPT' || signoff[0].approveMethod !== 'ALL'
          || signoff[0].approveRatio !== 100 || signoff[0].requireAllApprovals !== true
          || JSON.stringify(customDepartments(signoff[0].candidateSourceIds)) !== JSON.stringify(departments))
          throw new Error('本次会签部门必须逐部门全部通过，正式预检义务不一致')
        const actors = customValue(response.applicationCustomMatrix)
        for (const [stage, id] of [['MATRIX_APPROVAL', actors?.approverUserId], ['DOC_CONTROL_REVIEW', actors?.documentControlUserId]] as const) {
          const nodes = response.nodes.filter(node => node.stageCode === stage)
          if (!id || nodes.length !== 1 || nodes[0].candidateSourceType !== 'USER' || nodes[0].resolvedUserIds?.length !== 1
            || String(nodes[0].resolvedUserIds[0]) !== id || nodes[0].candidateSourceIds?.length !== 1 || String(nodes[0].candidateSourceIds[0]) !== id)
            throw new Error('本次批准人或文控审核人与正式预检不一致')
        }
      }
      const selected: ScopedApprovalMatrixSelection = { approvalSourceMode: 'APPLICATION_CUSTOM', selectedSignoffDepartmentIds: departments,
        applicationCustomMatrix: copy(response.applicationCustomMatrix ?? null),
        expectedApplicationCustomMatrixDigest: response.applicationCustomMatrixDigest ?? undefined,
        applicationCustomMatrixToken: response.applicationCustomMatrixToken ?? undefined,
        applicationCustomMatrixPhase: response.applicationCustomMatrixPhase ?? undefined,
        applicationContextKey: this.scope.applicationContextKey, executionContract: response.executionContract ?? undefined,
        expectedProcessDefinitionId: response.preparedProcessDefinitionId ?? undefined, expectedDefinitionDigest: response.preparedDefinitionDigest ?? undefined,
        expectedSupervisorSourceDigest: response.supervisorSourceDigest ?? undefined, supervisorSourceToken: response.supervisorSourceToken ?? undefined }
      this.customAccepted = requireApprovalMatrixSelection(selected, { phase: 'SAVE', actionType: this.scope.actionType })
      this.lastPrepared = { executionContract: selected.executionContract!, processDefinitionId: selected.expectedProcessDefinitionId!, definitionDigest: selected.expectedDefinitionDigest! }
      this.runtimeReady = response.ready === true; this.runtimeError = matrixRuntimeBlockerMessage(response); this.pending = false
      return true
    }
    if (request.request.prepareNewActionBaseline === true && (response.newActionBaselineRequired !== true || response.newActionBaselinePreparationAllowed !== true))
      throw new Error('服务器未确认本次升版新配置准备资格，未接受返回基线')
    const before = response.matrixBefore, after = response.matrixAfter
    if (!before || !after || !response.approvalMatrix || typeof response.matrixChangeRequired !== 'boolean'
      || response.derivationVersion !== 'DCC_PUBLIC_MATRIX_NATIVE_V1' || response.applicationActionType !== this.scope.actionType)
      throw new Error('审批矩阵来源尚未配置或正式预检响应缺失')
    validateApprovalMatrixBaseline(before, this.scope); validateApprovalMatrixBaseline(after, this.scope)
    validateApprovalMatrixBaseline(response.approvalMatrix, this.scope)
    if (response.routeId !== after.routeId || response.routeVersionNo !== after.routeVersionNo
      || response.matrixDigest !== after.matrixDigest || response.approvalMatrix.matrixDigest !== after.matrixDigest)
      throw new Error('审批矩阵当前版本与正式响应不一致')
    if (this.before && (this.before.matrixDigest !== before.matrixDigest || this.before.routeId !== before.routeId
      || this.before.routeVersionNo !== before.routeVersionNo || JSON.stringify(this.before) !== JSON.stringify(before)))
      throw new Error('本次原审批矩阵基线已变化，不能使用当前配置覆盖')
    const changed = before.matrixDigest !== after.matrixDigest
    if (response.matrixChangeRequired !== changed) throw new Error('审批矩阵变化事实不一致')
    const baselineToken = tokenText(response.approvalMatrixBaselineToken)
    const confirmationToken = changed ? tokenText(response.approvalMatrixConfirmationToken) : null
    const prepared = readPreparedDefinition(response)
    if (this.scope.controlledFileId && this.lastPrepared && prepared
      && (prepared.executionContract !== this.lastPrepared.executionContract || prepared.processDefinitionId !== this.lastPrepared.processDefinitionId
        || prepared.definitionDigest !== this.lastPrepared.definitionDigest))
      throw new Error('已保存申请的精确流程定义或执行合同不一致，不得隐式升级或替换')
    const native = this.scope.actionType === 'NEW' || this.scope.actionType === 'REVISION'
    const supervisor = native ? supervisorSourcePair(response.supervisorSourceDigest, response.supervisorSourceToken, response.ready === true) : undefined
    if (request.request.prepareNewActionBaseline === true && !prepared)
      throw new Error(matrixRuntimeBlockerMessage(response) || '本次升版目标的精确流程准备未返回，未接受新基线')
    if (request.request.prepareNewActionBaseline === true && prepared && (request.request.expectedProcessDefinitionId !== prepared.processDefinitionId
      || request.request.expectedDefinitionDigest !== prepared.definitionDigest))
      throw new Error('本次升版准备返回定义与目标已保存的精确流程准备不一致，未接受新基线')
    const oldPrepared = this.lastPrepared
    const oldSupervisor = this.lastSupervisorSource
    const previousTarget = this.targetIdentity()
    this.before = copy(before); this.after = copy(after); this.changed = changed
    this.baselineToken = baselineToken; this.confirmationToken = confirmationToken
    this.prepared = prepared
    this.supervisorSource = supervisor
    this.runtimeReady = response.ready === true
    this.runtimeError = matrixRuntimeBlockerMessage(response)
    if (supervisor) this.lastSupervisorSource = copy(supervisor)
    if (oldSupervisor && supervisor && oldSupervisor.digest !== supervisor.digest) this.supervisorNeedsConfirmation = true
    if (prepared) this.lastPrepared = copy(prepared)
    this.preparationError = prepared ? '' : matrixRuntimeBlockerMessage(response) || '本次正式流程定义尚未准备，请重新读取流程预检'
    if (oldPrepared && prepared && (oldPrepared.processDefinitionId !== prepared.processDefinitionId || oldPrepared.definitionDigest !== prepared.definitionDigest)) {
      this.previousPrepared = copy(oldPrepared); this.definitionNeedsConfirmation = true
    }
    this.pending = false
    if (previousTarget !== this.targetIdentity()) this.confirmationIdentity = ''
    return true
  }
  prepareNewActionBaseline(facts: NewActionBaselineEligibility, selected?: Array<number | string>): MatrixRequest {
    if (!isNewActionBaselinePreparationAllowed(facts, this.scope) || this.before && !this.newActionPreparation)
      throw new Error('本次新业务基线准备资格不成立，已保存基线不能覆盖')
    if (!this.lastPrepared) this.lastPrepared = { executionContract: facts.executionContract as PublicExecutionContract, processDefinitionId: definitionIdentity(facts.preparedProcessDefinitionId),
      definitionDigest: definitionDigest(facts.preparedDefinitionDigest) }
    else if (this.lastPrepared.processDefinitionId !== facts.preparedProcessDefinitionId || this.lastPrepared.definitionDigest !== facts.preparedDefinitionDigest)
      throw new Error('本次目标已保存流程准备身份已变化，请重新读取工作稿')
    this.newActionPreparation = true
    return this.begin(selected)
  }
  newActionSaveSelection(): ApprovalMatrixConfirmationSelection & { prepareNewActionBaseline?: boolean } {
    return { ...this.selection('SAVE'), ...(this.newActionPreparation ? { prepareNewActionBaseline: true } : {}) }
  }
  newActionBaselineSaved(actual: ApprovalMatrixBaselineVO) {
    if (!this.newActionPreparation || !this.before) throw new Error('本次没有待保存的新业务基线')
    validateApprovalMatrixBaseline(actual, this.scope)
    if (JSON.stringify(actual) !== JSON.stringify(this.before)) throw new Error('本次升版配置保存回显与正式准备基线不一致')
    this.newActionPreparation = false
  }
  private targetIdentity() { return JSON.stringify([this.scopeKey(), this.before?.matrixDigest,
    this.after?.routeId, this.after?.routeVersionNo, this.after?.matrixDigest, this.confirmationToken,
    this.prepared?.executionContract, this.prepared?.processDefinitionId, this.prepared?.definitionDigest, this.supervisorSource?.digest, this.supervisorSource?.token]) }
  async confirmChange(confirm: (rows: MatrixSummaryRow[]) => Promise<boolean>): Promise<boolean> {
    if (this.pending) throw new Error('审批矩阵正在重新读取，请等待当前预检完成')
    if (this.scope.approvalSourceMode === 'APPLICATION_CUSTOM') { if (!this.customAccepted) throw new Error('请先读取本次自定义预检'); return true }
    if (!this.after || !this.before) throw new Error('请先读取正式审批矩阵')
    if (!this.changed && !this.definitionNeedsConfirmation && !this.supervisorNeedsConfirmation) return true
    if (this.confirmationIdentity === this.targetIdentity()) return true
    const target = this.targetIdentity()
    const sequence = this.sequence
    if (!(await confirm(this.summary())) || this.pending || sequence !== this.sequence || target !== this.targetIdentity()) return false
    this.confirmationIdentity = target
    this.definitionNeedsConfirmation = false
    if (this.supervisorSource) this.supervisorNeedsConfirmation = false
    return true
  }
  invalidateConfirmation() { this.confirmationIdentity = '' }
  reject(error: unknown): string {
    this.confirmationIdentity = ''
    this.pending = true
    const rejection = readApprovalMatrixRejection(error)
    if (!rejection) throw new Error('当前错误不是已登记的矩阵业务拒绝')
    validateApprovalMatrixBaseline(rejection.matrixBefore, this.scope)
    validateApprovalMatrixBaseline(rejection.matrixAfter, this.scope)
    if (!this.before || this.before.matrixDigest !== rejection.matrixBefore.matrixDigest
      || this.before.routeId !== rejection.matrixBefore.routeId || this.before.routeVersionNo !== rejection.matrixBefore.routeVersionNo)
      throw new Error('矩阵拒绝响应的原基线与本次申请不一致，未接受其票据；请重新读取正式预检')
    // Error data is authoritative rejection evidence, never a substitute for fresh runtime readiness.
    return `${rejection.reasonCode}：${rejection.reasonCode === 'APPROVAL_MATRIX_EXPECTATION_STALE'
      ? '公共审批配置已再次变化，本次写入被拒绝' : '公共审批配置变化尚未明确确认，本次写入被拒绝'}`
  }
  selection(phase: 'SUBMIT' | 'SAVE' = 'SUBMIT'): ScopedApprovalMatrixSelection {
    if (this.pending) throw new Error('审批矩阵正在重新读取，请等待当前预检完成')
    if (this.scope.approvalSourceMode === 'APPLICATION_CUSTOM') {
      if (!this.customAccepted) throw new Error('请先读取本次自定义配置预检')
      if (phase === 'SUBMIT' && !this.runtimeReady) throw new Error(this.runtimeError || '本次自定义配置人员或部门尚未就绪')
      return requireApprovalMatrixSelection(this.customAccepted, { phase, actionType: this.scope.actionType })
    }
    if (!this.before || !this.after || !this.baselineToken) throw new Error('请先读取正式审批矩阵配置')
    if (!this.prepared) throw new Error(this.preparationError || '本次正式流程定义尚未准备')
    const native = this.scope.actionType === 'NEW' || this.scope.actionType === 'REVISION'
    if (phase === 'SUBMIT' && native && !this.runtimeReady) throw new Error(this.runtimeError || '直属主管正式来源或本次运行资格尚未就绪')
    if (phase === 'SUBMIT' && this.supervisorNeedsConfirmation) throw new Error('直属主管正式来源已变化，请重新核对并确认')
    if (this.definitionNeedsConfirmation) throw new Error('本次正式流程准备已变化，请重新核对并确认')
    if (this.changed && this.confirmationIdentity !== this.targetIdentity()) throw new Error('审批矩阵已变化，请核对确认前和当前配置并明确确认')
    const supervisor = this.supervisorNeedsConfirmation && phase === 'SAVE' ? undefined : this.supervisorSource
    return requireApprovalMatrixSelection({ expectedApprovalRouteId: this.after.routeId,
      executionContract: this.prepared.executionContract,
      expectedProcessDefinitionId: this.prepared.processDefinitionId, expectedDefinitionDigest: this.prepared.definitionDigest,
      expectedApprovalRouteVersionNo: this.after.routeVersionNo, expectedApprovalMatrixDigest: this.after.matrixDigest,
      approvalMatrixBaselineToken: this.baselineToken, approvalMatrixConfirmationToken: this.changed ? this.confirmationToken : null,
      matrixChangeConfirmed: this.changed, applicationContextKey: this.scope.applicationContextKey,
      ...(supervisor ? { expectedSupervisorSourceDigest: supervisor.digest, supervisorSourceToken: supervisor.token } : {}) },
      { phase, actionType: this.scope.actionType })
  }
  summary(): MatrixSummaryRow[] {
    const rows: MatrixSummaryRow[] = []
    if (this.supervisorNeedsConfirmation) rows.push({ label: '直属主管来源', value: '正式来源已变化，请重新核对本次直属主管及其部门后确认。' })
    const subjectTypes: Record<string, string> = { USER: '账号', DEPT: '部门', ROLE: '角色', POST: '岗位', POSITION: '岗位', DCC_POSITION: '文控岗位' }
    const stageTypes: Record<string, string> = { SIGNOFF: '会签', APPROVAL: '批准', DOC_CONTROL: '文控' }
    const flag = (value: boolean | null) => value === null ? '未记录' : value ? '是' : '否'
    for (const [label, value] of [['确认前', this.before], ['当前', this.after]] as const) {
      if (!value) throw new Error('审批矩阵原始对比尚未读取')
      rows.push({ label: `${label}审批配置`, value: `配置ID ${value.routeId}；版本 ${value.routeVersionNo}；生效时间 ${value.effectiveTime}；备注 ${value.routeRemark === null ? '未记录' : value.routeRemark}` })
      value.nodes.forEach(node => rows.push({ label: `${label}规则 ${node.id}`, value:
        `阶段 ${node.stageName ?? node.stageCode ?? '未记录'}；阶段类型 ${node.stageType === null ? '未记录' : stageTypes[node.stageType] ?? node.stageType}；阶段序号 ${node.stageNo ?? '未记录'}；阶段排序 ${node.stageOrder ?? '未记录'}；节点排序 ${node.sort ?? '未记录'}；标记 ${node.marker ?? '未记录'}；主体 ${node.subjectName ?? node.subjectLabel ?? '未记录'}；主体类型 ${node.subjectType === null ? '未记录' : subjectTypes[node.subjectType] ?? node.subjectType}；主体ID ${node.subjectId ?? '未记录'}；部门 ${node.subjectDepartmentPath ?? '未记录'}；候选来源 ${node.candidateSourceType === null ? '未记录' : subjectTypes[node.candidateSourceType] ?? node.candidateSourceType}；候选ID ${node.candidateSourceId ?? '未记录'}；原候选集合 ${node.candidateSourceIds ?? '未记录'}；批准规则 ${node.approveMethod ?? '未记录'}；比例 ${node.approveRatio ?? '未记录'}；必须参与 ${flag(node.required)}；全部批准 ${flag(node.requireAllApprovals)}；备注 ${node.ruleRemark === null ? '未记录' : node.ruleRemark}` }))
    }
    if (this.previousPrepared) rows.push({ label: '原流程准备', value: `定义身份 ${this.previousPrepared.processDefinitionId}；模型摘要 ${this.previousPrepared.definitionDigest}` })
    rows.push({ label: '本次流程准备', value: this.prepared
      ? `定义身份 ${this.prepared.processDefinitionId}；模型摘要 ${this.prepared.definitionDigest}` : this.preparationError || '未准备' })
    return rows
  }
}

export interface ApprovalMatrixRejection {
  reasonCode: 'APPROVAL_MATRIX_EXPECTATION_STALE' | 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED'
  matrixBefore: ApprovalMatrixBaselineVO; matrixAfter: ApprovalMatrixBaselineVO
  approvalMatrixBaselineToken: string; approvalMatrixConfirmationToken: string
}
const matrixErrorEnvelope = (error: unknown) => {
  const value = error as { code?: unknown; details?: unknown; data?: unknown; response?: { data?: { code?: unknown; data?: unknown } } }
  return { code: value?.response?.data?.code ?? value?.code, details: value?.response?.data?.data ?? value?.details ?? value?.data }
}
export const isApprovalMatrixStaleError = (error: unknown) => {
  const { code } = matrixErrorEnvelope(error)
  return code === 1080000363 || code === 1080000364 || code === '1080000363' || code === '1080000364'
}
export const readApprovalMatrixRejection = (error: unknown): ApprovalMatrixRejection | undefined => {
  if (!isApprovalMatrixStaleError(error)) return undefined
  const { code, details } = matrixErrorEnvelope(error)
  if (!details || typeof details !== 'object' || Array.isArray(details)) throw new Error('矩阵业务拒绝缺少正式比较载荷')
  const payload = details as ApprovalMatrixRejection
  const expectedReason = String(code) === '1080000363' ? 'APPROVAL_MATRIX_EXPECTATION_STALE' : 'APPROVAL_MATRIX_CHANGE_CONFIRMATION_REQUIRED'
  if (payload.reasonCode !== expectedReason) throw new Error('矩阵拒绝业务码与原因分类不一致')
  if (!payload.matrixBefore || !payload.matrixAfter) throw new Error('矩阵拒绝缺少正式确认前或当前配置')
  tokenText(payload.approvalMatrixBaselineToken); tokenText(payload.approvalMatrixConfirmationToken)
  return copy(payload)
}
export const matrixRuntimeBlockerMessage = (response: ControlledFileRouteReadinessVO | undefined): string => {
  if (!response || response.ready) return ''
  if (!Array.isArray(response.blockers) || !response.blockers.length) return '正式预检未返回可核对的运行资格原因'
  return response.blockers.map(blocker => `${blocker.stageName ? `${blocker.stageName}：` : ''}${blocker.userName ? `${blocker.userName}：` : ''}${blocker.reasonCode ? `[${blocker.reasonCode}] ` : ''}${blocker.message}`).join('；')
}

export const refreshApprovalMatrixRejection = async (state: UploadApprovalMatrixState | undefined,
  error: unknown, refresh: () => Promise<string>): Promise<string> => {
  let reason: string
  try {
    if (!state) throw new Error('本次申请原矩阵上下文未读取')
    reason = state.reject(error)
  } catch (cause) { return `矩阵业务拒绝载荷无法核验，未接受其票据或刷新配置：${cause instanceof Error ? cause.message : String(cause)}` }
  try {
    const runtimeBlocker = await refresh()
    return `${reason}；请重新核对正式预检并确认本次配置${runtimeBlocker ? `；当前运行资格阻塞：${runtimeBlocker}` : ''}`
  } catch (cause) { return `${reason}；重新读取失败：${cause instanceof Error ? cause.message : String(cause)}` }
}
