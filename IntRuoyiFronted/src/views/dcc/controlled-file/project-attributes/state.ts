export type ApplicationType = 'UPLOAD' | 'REVISION' | 'OBSOLETE'
export interface ProjectAttributes {
  targetMarkets: string[]
  otherMarket?: string
  licenseHolder?: string
  actualManufacturer?: string
  documentTransfer?: string
  transferTo?: string
}
export interface AttributeSnapshot {
  projectId: string
  applicationType: ApplicationType
  defaultSource: ProjectAttributes
  actual: ProjectAttributes
}
export interface AttributeState {
  applicationType: ApplicationType
  projectId?: string
  defaultSource?: ProjectAttributes
  actual?: ProjectAttributes
  dirty: boolean
  error: string
  requestSequence: number
}
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))
export const emptyAttributes = (): ProjectAttributes => ({ targetMarkets: [] })
export const validateAttributes = (value: ProjectAttributes): ProjectAttributes => {
  const markets = ['NMPA', 'CE', 'FDA', 'MADSAP', 'OTHER', 'NA']
  if (!value || !Array.isArray(value.targetMarkets) || !value.targetMarkets.length ||
    value.targetMarkets.some(item => !markets.includes(item)) ||
    new Set(value.targetMarkets).size !== value.targetMarkets.length ||
    (value.targetMarkets.includes('NA') && value.targetMarkets.length !== 1)) {
    throw new Error('目标市场至少选一项，不适用与其他市场互斥')
  }
  for (const field of ['licenseHolder', 'actualManufacturer'] as const) {
    if (!['Y', 'N', 'NA'].includes(value[field] || '')) throw new Error('请分别选择是否为注册人、是否为生产方')
  }
  if (!['Y', 'N'].includes(value.documentTransfer || '')) throw new Error('请选择文件转移是或否')
  if (value.targetMarkets.includes('OTHER') !== Boolean(value.otherMarket?.trim()) ||
    (value.otherMarket?.length || 0) > 512) throw new Error('其他市场说明与选择不匹配')
  if ((value.documentTransfer === 'Y') !== Boolean(value.transferTo?.trim()) ||
    (value.transferTo?.length || 0) > 512) throw new Error('转移至与文件转移选择不匹配')
  return value
}
export const changeMarkets = (value: ProjectAttributes, selected: string[]): ProjectAttributes => {
  const next = clone(value)
  // 新选 NA 时移除其他项；从 NA 切到实际市场时移除 NA
  next.targetMarkets = selected.includes('NA') && !value.targetMarkets.includes('NA')
    ? ['NA'] : selected.filter(item => item !== 'NA' || selected.length === 1)
  if (!next.targetMarkets.includes('OTHER')) delete next.otherMarket
  return next
}
export const changeTransfer = (value: ProjectAttributes, choice: string): ProjectAttributes => {
  const next = { ...clone(value), documentTransfer: choice }
  if (choice === 'N') delete next.transferTo
  return next
}
export const createAttributeState = (applicationType: ApplicationType, saved?: AttributeSnapshot): AttributeState => {
  if (saved && saved.applicationType !== applicationType) throw new Error('申请属性动作不匹配')
  if (saved) { validateAttributes(saved.defaultSource); validateAttributes(saved.actual) }
  return {
    applicationType, projectId: saved?.projectId,
    defaultSource: saved ? clone(saved.defaultSource) : undefined,
    actual: saved ? clone(saved.actual) : undefined,
    dirty: Boolean(saved), error: '', requestSequence: 0
  }
}
export const editActual = (state: AttributeState, actual: ProjectAttributes) => {
  state.requestSequence++
  state.actual = clone(actual); state.dirty = true
}
const applyProjectDefaults = async (
  state: AttributeState, projectId: string,
  load: (projectId: string) => Promise<ProjectAttributes>,
  confirm: () => Promise<boolean>, forceConfirm: boolean
): Promise<boolean> => {
  const sequence = ++state.requestSequence
  try {
    if ((forceConfirm || state.dirty) && !await confirm()) return false
    if (sequence !== state.requestSequence) return false
    const defaults = validateAttributes(await load(projectId))
    if (sequence !== state.requestSequence) return false
    state.projectId = projectId; state.defaultSource = clone(defaults); state.actual = clone(defaults)
    state.dirty = false; state.error = ''
    return true
  } catch (error) {
    // 已被新项目/手改替代的请求按取消结束，不能污染新的正式上下文。
    if (sequence !== state.requestSequence) return false
    state.error = error instanceof Error ? error.message : String(error)
    throw error
  }
}
export const loadProjectDefaults = (
  state: AttributeState, projectId: string,
  load: (projectId: string) => Promise<ProjectAttributes>,
  confirm: () => Promise<boolean>
): Promise<boolean> => applyProjectDefaults(state, projectId, load, confirm, false)
export const restoreDefaults = (
  state: AttributeState, load: (projectId: string) => Promise<ProjectAttributes>,
  confirm: () => Promise<boolean>
): Promise<boolean> => {
  if (!state.projectId) return Promise.reject(new Error('请先选择项目'))
  // 在确认前锁定项目和请求序号，不能确认后借用另一项目的身份。
  return applyProjectDefaults(state, state.projectId, load, confirm, true)
}
export const buildSnapshot = (state: AttributeState): AttributeSnapshot => {
  if (state.error) throw new Error(state.error)
  if (!state.projectId || !state.defaultSource || !state.actual) throw new Error('请先读取项目默认属性')
  return clone({
    projectId: state.projectId, applicationType: state.applicationType,
    defaultSource: validateAttributes(state.defaultSource), actual: validateAttributes(state.actual)
  })
}

export interface ApplicationAttributesRow {
  projectCodeId: number | string
  applicationType: ApplicationType
  applicationId: number | string
  applicationRound: number
  defaultSourceJson: string
  actualAttributesJson: string
  submitted: boolean
}
export const fromApplicationSnapshot = (
  row: ApplicationAttributesRow, projectId: number | string, action: ApplicationType, applicationId: number | string
): AttributeSnapshot => {
  if (!row || String(row.projectCodeId) !== String(projectId) || row.applicationType !== action
    || String(row.applicationId) !== String(applicationId) || !Number.isInteger(row.applicationRound)
    || row.applicationRound <= 0 || typeof row.submitted !== 'boolean') throw new Error('正式申请属性身份或轮次不匹配')
  if (!row.defaultSourceJson || !row.actualAttributesJson) throw new Error('历史申请属性未记录，不能用当前项目默认补齐')
  return {
    projectId: String(projectId), applicationType: action,
    defaultSource: validateAttributes(JSON.parse(row.defaultSourceJson)),
    actual: validateAttributes(JSON.parse(row.actualAttributesJson))
  }
}
