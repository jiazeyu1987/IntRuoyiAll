import type { DirectSupervisorRelation, DirectSupervisorSave } from '@/api/system/user/directSupervisor'
export interface SupervisorCandidate { id: number | string; username: string; nickname: string }
export interface SupervisorPort {
  get(userId: string): Promise<DirectSupervisorRelation>
  candidates(): Promise<SupervisorCandidate[]>
  save(value: DirectSupervisorSave): Promise<DirectSupervisorRelation>
}
export const createSupervisorState = () => ({ visible: false, loading: false, saving: false, ready: false, error: '', sequence: 0,
  userId: '', tenantId: '', relation: undefined as DirectSupervisorRelation | undefined, candidates: [] as SupervisorCandidate[],
  selectedId: undefined as string | undefined, operation: 'SET' as 'SET' | 'CLEAR', reason: '', notice: '' })
export type SupervisorState = ReturnType<typeof createSupervisorState>
const exactLong = (value: unknown, zero = false): string => {
  if (typeof value !== 'string' && (typeof value !== 'number' || !Number.isSafeInteger(value))) throw new Error('账号或关系版本身份不完整')
  const text = String(value)
  if (!(zero ? /^(0|[1-9][0-9]*)$/ : /^[1-9][0-9]*$/).test(text) || BigInt(text) > 9223372036854775807n) throw new Error('账号或关系版本不合法')
  return text
}
const validate = (value: DirectSupervisorRelation, tenant: string, user: string) => {
  if (!value || exactLong(value.tenantId, true) !== tenant || exactLong(value.userId) !== user
    || !value.username?.trim() || !value.nickname?.trim()) throw new Error('直属主管响应身份不匹配，请重新读取核对')
  exactLong(value.relationVersion, true)
  if (value.supervisorUserId !== null) {
    if (exactLong(value.supervisorUserId) === user) throw new Error('直属主管不能是本人')
  }
  return value
}
const causeText = (cause: unknown) => cause instanceof Error ? cause.message : String(cause)
export const closeSupervisor = (state: SupervisorState) => { state.sequence++; state.visible = false; state.ready = false; state.loading = false; state.saving = false }
export const openSupervisor = async (state: SupervisorState, tenant: unknown, user: unknown, port: SupervisorPort): Promise<boolean> => {
  const sequence = ++state.sequence
  Object.assign(state, { visible: true, ready: false, loading: true, saving: false, error: '', notice: '', relation: undefined,
    candidates: [], selectedId: undefined, operation: 'SET', reason: '', tenantId: '', userId: '' })
  try {
    const tenantId = exactLong(tenant, true), userId = exactLong(user)
    state.tenantId = tenantId; state.userId = userId
    const [relation, candidates] = await Promise.all([port.get(userId), port.candidates()])
    if (sequence !== state.sequence || !state.visible) return false
    validate(relation, tenantId, userId)
    if (!Array.isArray(candidates)) throw new Error('直属主管候选响应不完整')
    const ids = new Set<string>()
    const checked = candidates.filter(row => {
      const id = exactLong(row.id)
      if (!row.username?.trim() || !row.nickname?.trim() || ids.has(id)) throw new Error('直属主管候选身份不完整或重复')
      ids.add(id); return id !== userId
    })
    state.relation = relation; state.candidates = checked
    state.selectedId = relation.supervisorUserId === null ? undefined : String(relation.supervisorUserId)
    state.ready = true
    return true
  } catch (cause) {
    if (sequence === state.sequence) state.error = causeText(cause)
    return false
  } finally { if (sequence === state.sequence) state.loading = false }
}
export const clearSupervisor = (state: SupervisorState) => {
  if (!state.ready || state.loading || state.saving) return
  state.operation = 'CLEAR'; state.selectedId = undefined; state.notice = '待保存：清空直属主管'
}
export const submitSupervisor = async (state: SupervisorState, port: SupervisorPort): Promise<DirectSupervisorRelation | undefined> => {
  if (!state.visible || !state.ready || state.loading || state.saving || !state.relation) return
  const sequence = state.sequence, relation = state.relation, tenant = state.tenantId, user = state.userId
  state.error = ''
  let payload: DirectSupervisorSave
  try {
    const selected = state.operation === 'CLEAR' ? null : exactLong(state.selectedId)
    if (selected === user || (selected !== null && !state.candidates.some(row => String(row.id) === selected))) throw new Error('请选择当前有效直属主管，或点击明确清空')
    if (!state.reason.trim() || state.reason.length > 500) throw new Error('请填写本次变更原因')
    payload = { userId: user, expectedRelationVersion: exactLong(relation.relationVersion, true), operation: state.operation,
      supervisorUserId: selected, changeReason: state.reason.trim() }
  } catch (cause) { state.error = causeText(cause); return }
  state.saving = true
  try {
    const response = await port.save(payload)
    // The original server command may have committed. Its result belongs only to its original visible generation.
    if (sequence !== state.sequence || !state.visible) return
    const saved = validate(response, tenant, user)
    const same = relation.supervisorUserId === payload.supervisorUserId
    const expected = same ? BigInt(payload.expectedRelationVersion) : BigInt(payload.expectedRelationVersion) + 1n
    if (saved.supervisorUserId !== payload.supervisorUserId || BigInt(saved.relationVersion) !== expected) {
      if (sequence === state.sequence) state.ready = false
      throw new Error('直属主管保存响应版本或关系不匹配，请重新读取核对')
    }
    if (sequence === state.sequence) { state.relation = saved; state.visible = false; state.ready = false }
    return saved
  } catch (cause) {
    if (sequence === state.sequence) { state.error = causeText(cause); if (state.error.includes('响应身份')) state.ready = false }
    return
  } finally { if (sequence === state.sequence) state.saving = false }
}
