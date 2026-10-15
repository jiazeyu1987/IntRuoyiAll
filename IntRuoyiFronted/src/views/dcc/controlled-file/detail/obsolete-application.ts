import type { ControlledFileObsoleteApplicationVO, ControlledFileObsoleteApplicationReadVO,
  ControlledFileObsoleteReworkSourceVO, ControlledFileObsoleteReworkSourceExpectation,
  ControlledFileObsoleteReqVO } from '@/api/dcc/controlledFile/workflow'
import type { ProjectAttributes } from '../project-attributes/state'

export interface ObsoleteApplicationScope {
  key: string; fileId: string; projectId: string; versionNo: string; tenantId?: string; actorId?: string
}
export interface ObsoleteApplicationInput { reason: string; selectedSignoffDepartmentIds: string[]; projectAttributes?: ProjectAttributes }
export type ObsoleteMatrixSelection = Pick<ControlledFileObsoleteReqVO, 'expectedApprovalRouteId' | 'expectedApprovalRouteVersionNo'
  | 'expectedApprovalMatrixDigest' | 'approvalMatrixBaselineToken' | 'approvalMatrixConfirmationToken' | 'matrixChangeConfirmed'>
const canonical = (value: unknown): string => {
  const sorted = (input: unknown): unknown => Array.isArray(input) ? input.map(sorted)
    : input && typeof input === 'object'
      ? Object.fromEntries(Object.entries(input).sort(([a], [b]) => a.localeCompare(b)).map(([key, item]) => [key, sorted(item)]))
      : input
  return JSON.stringify(sorted(value))
}
const copy = <T>(value: T): T => JSON.parse(JSON.stringify(value))
const business = (input: ObsoleteApplicationInput) => canonical({
  reason: input.reason.trim(), selectedSignoffDepartmentIds: [...input.selectedSignoffDepartmentIds].sort(),
  projectAttributes: input.projectAttributes
})
const exactText = (value: unknown): value is string => typeof value === 'string' && !!value && value.trim() === value
const identity = (value: unknown): string => {
  if (typeof value !== 'string' || !/^[1-9][0-9]*$/.test(value) || BigInt(value) > 9223372036854775807n)
    throw new Error('作废申请身份缺失或不精确')
  return value
}
const digest = (value: unknown): value is string => typeof value === 'string' && /^[a-f0-9]{64}$/.test(value)
export const obsoleteReworkSourceExpectation = (
  value: ControlledFileObsoleteReworkSourceVO | ControlledFileObsoleteApplicationVO
): ControlledFileObsoleteReworkSourceExpectation => {
  const id = identity(value.reworkSourceFormInstanceId)
  if (!exactText(value.reworkSourceFormInstanceCode) || !exactText(value.reworkSourceBpmProcessInstanceId)
    || !Number.isSafeInteger(value.reworkSourceAttributeRound) || value.reworkSourceAttributeRound! < 1)
    throw new Error('旧作废申请来源身份不完整')
  return { reworkSourceFormInstanceId: id, reworkSourceFormInstanceCode: value.reworkSourceFormInstanceCode,
    reworkSourceBpmProcessInstanceId: value.reworkSourceBpmProcessInstanceId,
    reworkSourceAttributeRound: value.reworkSourceAttributeRound! }
}
const sourceFive = (value: ControlledFileObsoleteReworkSourceVO | ControlledFileObsoleteApplicationVO) => {
  if (!digest(value.reworkSourceDigest)) throw new Error('旧作废申请来源摘要缺失')
  return { ...obsoleteReworkSourceExpectation(value), reworkSourceDigest: value.reworkSourceDigest }
}

/** The server entry, old source and new writable form have separate identities. */
export class ObsoleteApplicationSession {
  draft: ControlledFileObsoleteApplicationVO | undefined
  source: ControlledFileObsoleteApplicationVO | undefined
  candidates: ControlledFileObsoleteReworkSourceVO[] = []
  sourceSelectionRequired = false
  sourceRefreshRequired = false
  idempotencyKey = ''
  busy = false
  error = ''
  private sequence = 0
  private loaded = false
  constructor(
    private scope: () => ObsoleteApplicationScope,
    private read: (fileId: string, source?: ControlledFileObsoleteReworkSourceExpectation) => Promise<ControlledFileObsoleteApplicationReadVO | null>,
    private write: (fileId: string, data: ControlledFileObsoleteReqVO) => Promise<ControlledFileObsoleteApplicationVO>,
    private createKey: () => string,
    private currentInput?: () => ObsoleteApplicationInput
  ) {}
  clear() {
    this.sequence++; this.draft = undefined; this.source = undefined; this.candidates = []
    this.sourceSelectionRequired = false; this.sourceRefreshRequired = false; this.loaded = false
    this.idempotencyKey = ''; this.busy = false; this.error = ''
  }
  invalidateInputs() { this.sequence++; this.busy = false; this.error = '' }
  private current(sequence: number, scope: ObsoleteApplicationScope) {
    return this.sequence === sequence && canonical(this.scope()) === canonical(scope)
  }
  private requireScope(response: ControlledFileObsoleteApplicationReadVO, scope: ObsoleteApplicationScope) {
    if (!response || response.controlledFileId !== scope.fileId || response.dccProjectCodeId !== scope.projectId
      || response.versionNo !== scope.versionNo) throw new Error('作废申请响应与所选文件版本身份不一致')
  }
  private requireForm(form: ControlledFileObsoleteApplicationVO) {
    identity(form.id)
    if (!exactText(form.instanceCode) || !Number.isSafeInteger(form.attributeRound) || form.attributeRound < 1
      || !exactText(form.applicationContextKey) || !form.defaultSource || !form.actualAttributes
      || !Array.isArray(form.selectedSignoffDepartmentIds)) throw new Error('正式作废申请完整输入或属性轮次缺失')
  }
  private requireSource(source: ControlledFileObsoleteApplicationVO, scope: ObsoleteApplicationScope) {
    this.requireScope(source, scope); this.requireForm(source)
    const expected = sourceFive(source), relations = source.reworkSourceRelations
    if (source.applicationEntryMode !== 'REWORK_SOURCE' || !['REJECTED', 'ABANDONED'].includes(source.status)
      || source.status !== source.reworkSourceStatus || source.preparationReady !== false || source.attributesSubmitted !== true
      || source.id !== expected.reworkSourceFormInstanceId || source.instanceCode !== expected.reworkSourceFormInstanceCode
      || source.bpmProcessInstanceId !== expected.reworkSourceBpmProcessInstanceId || source.attributeRound !== expected.reworkSourceAttributeRound
      || source.reworkSourceVersionNo !== scope.versionNo || !source.reworkSourceApprovalMatrix
      || !digest(source.reworkSourceApprovalMatrix.matrixDigest) || !relations || relations.fileId !== scope.fileId
      || relations.projectId !== scope.projectId || (scope.tenantId && relations.tenantId !== scope.tenantId)
      || relations.applicationType !== 'OBSOLETE' || relations.attributeRound !== expected.reworkSourceAttributeRound
      || relations.bpmRound !== expected.reworkSourceBpmProcessInstanceId || typeof relations.recorded !== 'boolean'
      || !Array.isArray(relations.originalUploadSelections) || !Array.isArray(relations.completeSelections) || !Array.isArray(relations.stages)
      || !digest(relations.relationDigest) || relations.relationDigest !== source.reworkSourceRelationDigest)
      throw new Error('旧终止申请、冻结属性、审批规则或关联来源证据不完整')
  }
  private adoptRead(response: ControlledFileObsoleteApplicationReadVO | null, scope: ObsoleteApplicationScope) {
    if (response) this.requireScope(response, scope)
    if (!response) {
      const key = this.createKey()
      if (!exactText(key)) throw new Error('新作废申请请求键缺失')
      this.idempotencyKey = key
    } else if (response.applicationEntryMode === 'SOURCE_SELECTION_REQUIRED') {
      if (!Array.isArray(response.reworkSourceCandidates) || response.reworkSourceCandidates.length < 2
        || response.preparationReady !== false || !exactText(response.unavailableReason)) throw new Error('作废重办候选来源事实不完整')
      const seen = new Set<string>()
      for (const candidate of response.reworkSourceCandidates) {
        const expected = obsoleteReworkSourceExpectation(candidate), id = String(expected.reworkSourceFormInstanceId)
        if (seen.has(id) || !['REJECTED', 'ABANDONED'].includes(candidate.reworkSourceStatus)
          || candidate.reworkSourceVersionNo !== scope.versionNo || typeof candidate.available !== 'boolean'
          || (candidate.available ? !digest(candidate.reworkSourceDigest) || candidate.unavailableReason != null : !exactText(candidate.unavailableReason)))
          throw new Error('作废重办候选来源资格或身份不完整')
        seen.add(id)
      }
      this.candidates = copy(response.reworkSourceCandidates); this.sourceSelectionRequired = true
    } else if (response.applicationEntryMode === 'REWORK_SOURCE') {
      this.requireSource(response, scope)
      const key = this.createKey()
      if (!exactText(key) || key === response.idempotencyKey) throw new Error('重办必须使用独立新申请请求键')
      this.source = copy(response); this.idempotencyKey = key
    } else if (response.applicationEntryMode === 'DRAFT') {
      this.requireForm(response)
      if (response.status !== 'DRAFT' || response.bpmProcessInstanceId != null || response.attributesSubmitted !== false
        || !exactText(response.idempotencyKey)) throw new Error('作废入口不是可编辑正式草稿')
      this.draft = response; this.idempotencyKey = response.idempotencyKey
    } else throw new Error('作废申请入口类型缺失或未知')
    this.loaded = true
  }
  async load(): Promise<boolean> {
    this.clear()
    const sequence = this.sequence, scope = { ...this.scope() }
    const originalInput = this.currentInput ? business(this.currentInput()) : undefined
    this.busy = true
    try {
      const response = await this.read(scope.fileId)
      if (!this.current(sequence, scope)) return false
      if (originalInput !== undefined && originalInput !== business(this.currentInput!())) throw new Error('申请内容已改变，请重新读取申请入口')
      this.adoptRead(response, scope)
      return true
    } catch (cause) {
      if (this.current(sequence, scope)) this.error = cause instanceof Error ? cause.message : String(cause)
      return false
    } finally { if (this.current(sequence, scope)) this.busy = false }
  }
  async selectSource(candidate: ControlledFileObsoleteReworkSourceVO): Promise<boolean> {
    if (this.busy || !this.sourceSelectionRequired) return false
    const sequence = ++this.sequence, scope = { ...this.scope() }
    const originalInput = this.currentInput ? business(this.currentInput()) : undefined
    this.error = ''
    try {
      const known = this.candidates.find(item => canonical(obsoleteReworkSourceExpectation(item)) === canonical(obsoleteReworkSourceExpectation(candidate)))
      if (!known || !known.available) throw new Error(known?.unavailableReason || '请明确选择可用的旧作废申请')
      this.busy = true
      const response = await this.read(scope.fileId, obsoleteReworkSourceExpectation(known))
      if (!this.current(sequence, scope)) return false
      if (originalInput !== undefined && originalInput !== business(this.currentInput!())) throw new Error('申请内容已改变，请重新读取所选来源')
      if (!response || response.applicationEntryMode !== 'REWORK_SOURCE' || canonical(sourceFive(response)) !== canonical(sourceFive(known))) {
        this.sourceRefreshRequired = true
        throw new Error('所选作废来源已改变，请重新读取候选来源')
      }
      this.adoptRead(response, scope); this.sourceSelectionRequired = false
      return true
    } catch (cause) {
      if (this.current(sequence, scope)) this.error = cause instanceof Error ? cause.message : String(cause)
      return false
    } finally { if (this.current(sequence, scope)) this.busy = false }
  }
  isSaved(input: ObsoleteApplicationInput): boolean {
    return !!this.draft && this.draft.status === 'DRAFT' && business(input) === business({ reason: this.draft.reason,
      selectedSignoffDepartmentIds: this.draft.selectedSignoffDepartmentIds!, projectAttributes: this.draft.actualAttributes })
  }
  async execute(phase: ControlledFileObsoleteReqVO['phase'], input: ObsoleteApplicationInput,
    prepareExecution?: boolean, selection?: ObsoleteMatrixSelection): Promise<boolean> {
    if (this.busy) return false
    const sequence = this.sequence, scope = { ...this.scope() }, original = business(input)
    const unchanged = () => original === business(input) && (!this.currentInput || original === business(this.currentInput()))
    this.error = ''
    try {
      if (!this.loaded || !input.reason.trim()) throw new Error('请填写作废原因并读取正式申请入口')
      if (this.sourceSelectionRequired) throw new Error('请先明确选择旧作废申请来源')
      if (this.sourceRefreshRequired) throw new Error('旧来源已改变，请重新读取申请入口')
      if (!this.idempotencyKey) throw new Error('作废申请请求键缺失')
      if (phase === 'OPEN' && this.draft) throw new Error('已有正式草稿，请保存本次修改')
      if (phase !== 'OPEN' && !this.draft) throw new Error('请先明确重新发起并生成新作废草稿')
      if (this.draft && this.draft.status !== 'DRAFT') throw new Error('本次申请已提交，请重新读取正式状态')
      if ((phase === 'PREVIEW' || (phase === 'SAVE' && prepareExecution === true)) && !this.isSaved(input))
        throw new Error('内容已修改，请先保存完整草稿，再核对审批规则和准备审批')
      if (phase === 'SUBMIT') {
        if (!this.draft?.preparationReady) throw new Error('本次作废执行未准备或来源尚未就绪')
        if (!this.isSaved(input)) throw new Error('内容已修改，请先保存并重新核对执行准备')
      }
      const source = this.source, target = this.draft
      this.busy = true
      if (phase === 'OPEN' && source) {
        try {
          const latest = await this.read(scope.fileId, obsoleteReworkSourceExpectation(source))
          if (!this.current(sequence, scope)) return false
          if (!latest || latest.applicationEntryMode !== 'REWORK_SOURCE') throw new Error('旧来源已改变，请重新读取申请入口')
          this.requireSource(latest, scope)
          if (canonical(sourceFive(latest)) !== canonical(sourceFive(source))) throw new Error('旧来源摘要已改变，请重新读取申请入口')
        } catch (cause) {
          if (this.current(sequence, scope)) this.sourceRefreshRequired = true
          throw cause
        }
      }
      if (!unchanged()) throw new Error('申请内容已改变，请重新核对')
      const data: ControlledFileObsoleteReqVO = copy({
        ...input, reason: input.reason.trim(), idempotencyKey: this.idempotencyKey, phase,
        ...(phase === 'OPEN' ? (source ? sourceFive(source) : {}) : {
          formInstanceId: target!.id, formInstanceCode: target!.instanceCode,
          attributeRound: target!.attributeRound, applicationContextKey: target!.applicationContextKey }),
        ...(phase === 'SAVE' ? { prepareExecution } : {}), ...selection
      })
      const response = await this.write(scope.fileId, data)
      if (!this.current(sequence, scope)) return false
      if (!unchanged()) throw new Error('申请内容已改变，请重新读取已保存草稿并核对')
      this.requireScope(response, scope); this.requireForm(response)
      if (response.applicationEntryMode !== 'DRAFT') throw new Error('作废写入未返回真实新草稿')
      if (response.idempotencyKey !== data.idempotencyKey) throw new Error('作废写入响应改变了本次申请请求键')
      if (source && (response.id === source.id || response.instanceCode === source.instanceCode
        || response.attributeRound <= source.attributeRound))
        throw new Error('重办响应没有独立新表单、数字轮次和原来源')
      if (target && (response.id !== target.id || response.instanceCode !== target.instanceCode || response.attributeRound !== target.attributeRound
        || response.applicationContextKey !== target.applicationContextKey || response.idempotencyKey !== target.idempotencyKey))
        throw new Error('作废响应改变了本次正式草稿身份')
      const matrix = source?.reworkSourceApprovalMatrix || target?.reworkSourceApprovalMatrix
      const provenance = source || (target?.reworkSourceFormInstanceId ? target : undefined)
      if (provenance && (canonical(sourceFive(response)) !== canonical(sourceFive(provenance))
        || response.reworkSourceApprovalMatrix?.matrixDigest !== provenance.reworkSourceApprovalMatrix?.matrixDigest
        || response.reworkSourceRelationDigest !== provenance.reworkSourceRelationDigest
        || !response.reworkSourceRelations || response.reworkSourceRelations.relationDigest !== provenance.reworkSourceRelationDigest))
        throw new Error('重办响应丢失或改变了原申请、原规则和真实关联来源')
      if (matrix && ((response.approvalMatrixAfter && response.approvalMatrixAfter.matrixDigest !== matrix.matrixDigest)
        || (response.approvalMatrixBefore && response.approvalMatrixBefore.matrixDigest !== matrix.matrixDigest)))
        throw new Error('同类型重办审批规则改变了旧申请原规则')
      if (phase === 'SUBMIT' ? !['IN_APPROVAL', 'EFFECTIVE'].includes(response.status) || !exactText(response.bpmProcessInstanceId)
        || response.attributesSubmitted !== true || (!!provenance && response.bpmProcessInstanceId === provenance.reworkSourceBpmProcessInstanceId)
        : response.status !== 'DRAFT' || response.bpmProcessInstanceId != null || response.attributesSubmitted !== false)
        throw new Error('作废阶段响应的正式状态或属性冻结事实不一致')
      this.draft = response; this.source = undefined; this.sourceRefreshRequired = false
      this.idempotencyKey = response.idempotencyKey
      return true
    } catch (cause) {
      if (this.current(sequence, scope)) {
        this.error = cause instanceof Error ? cause.message : String(cause)
        if ((this.source || this.draft?.reworkSourceFormInstanceId) && this.error.startsWith('DCC_OBSOLETE_REWORK_'))
          this.sourceRefreshRequired = true
      }
      return false
    } finally { if (this.current(sequence, scope)) this.busy = false }
  }
}
