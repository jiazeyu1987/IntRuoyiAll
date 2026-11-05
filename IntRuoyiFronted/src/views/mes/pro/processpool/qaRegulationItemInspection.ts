export type QaItemDisplayInspectionType = 'FIRST' | 'PATROL' | 'FINAL'
export type QaItemPersistedInspectionType = 'FIRST' | 'PATROL' | 'FINAL'

export interface QaItemInspectionSource {
  applicableInspectionTypes: readonly string[]
  firstInspectionQuantity?: number
  finalInspectionQuantity?: number
  inspectionLevel?: string
  aql?: number
}

export interface QaItemInspectionState {
  finalInspectionEnabled: boolean
  firstInspectionEnabled: boolean
  firstInspectionQuantity?: number
  finalInspectionQuantity?: number
  patrolInspectionEnabled: boolean
  inspectionLevel?: string
  aql?: number
}

export interface QaItemInspectionPayload {
  applicableInspectionTypes: QaItemPersistedInspectionType[]
  firstInspectionQuantity?: number
  finalInspectionQuantity?: number
  inspectionLevel?: string
  aql?: number
}

const isPatrolInspectionType = (inspectionType: string) =>
  inspectionType === 'PATROL'

export const createQaItemInspectionState = (
  source: QaItemInspectionSource
): QaItemInspectionState => {
  const applicableInspectionTypes = new Set(source.applicableInspectionTypes)
  return {
    finalInspectionEnabled: applicableInspectionTypes.has('FINAL'),
    finalInspectionQuantity: source.finalInspectionQuantity,
    firstInspectionEnabled: applicableInspectionTypes.has('FIRST'),
    firstInspectionQuantity: source.firstInspectionQuantity,
    patrolInspectionEnabled: [...applicableInspectionTypes].some(isPatrolInspectionType),
    inspectionLevel: source.inspectionLevel,
    aql: source.aql
  }
}

const requireInspectionQuantity = (value: number | undefined, itemName: string, label: string) => {
  if (!Number.isSafeInteger(Number(value)) || Number(value) <= 0) {
    throw new Error(`${itemName}的${label}数量必须是大于 0 的整数`)
  }
  return Number(value)
}

const levels = ['I', 'II', 'III', 'S1', 'S2', 'S3', 'S4']
const requireInspectionLevel = (value: string | undefined, itemName: string) => {
  if (!value || !levels.includes(value)) throw new Error(`${itemName}请选择有效的巡检检验水平`)
  return value
}

export const resolveQaItemInspectionPayload = (
  item: QaItemInspectionState,
  finalInspectionApplicable: boolean,
  itemName: string
): QaItemInspectionPayload => {
  const applicableInspectionTypes: QaItemPersistedInspectionType[] = []
  const firstInspectionQuantity = item.firstInspectionEnabled
    ? requireInspectionQuantity(item.firstInspectionQuantity, itemName, '首检')
    : undefined
  const inspectionLevel = item.patrolInspectionEnabled
    ? requireInspectionLevel(item.inspectionLevel, itemName)
    : undefined

  if (item.firstInspectionEnabled) {
    applicableInspectionTypes.push('FIRST')
  }
  if (item.patrolInspectionEnabled) {
    applicableInspectionTypes.push('PATROL')
  }
  if (finalInspectionApplicable) {
    applicableInspectionTypes.push('FINAL')
  }
  if (applicableInspectionTypes.length === 0) {
    throw new Error(`${itemName}至少需要启用一种检验类型`)
  }

  return {
    applicableInspectionTypes,
    firstInspectionQuantity,
    finalInspectionQuantity: finalInspectionApplicable ? requireInspectionQuantity(item.finalInspectionQuantity, itemName, '末检') : undefined,
    inspectionLevel,
    aql: item.aql
  }
}

export const isQaItemInspectionConfigurationComplete = (
  item: QaItemInspectionState,
  finalInspectionApplicable: boolean
) =>
  (!item.firstInspectionEnabled ||
    (Number.isSafeInteger(Number(item.firstInspectionQuantity)) &&
      Number(item.firstInspectionQuantity) > 0)) &&
  (!item.patrolInspectionEnabled ||
    levels.includes(item.inspectionLevel ?? '')) &&
  (!finalInspectionApplicable || (Number.isSafeInteger(item.finalInspectionQuantity) && Number(item.finalInspectionQuantity) > 0)) &&
  (item.firstInspectionEnabled || item.patrolInspectionEnabled || finalInspectionApplicable)

export const resolveQaItemDisplayInspectionTypes = (
  item: QaItemInspectionState,
  finalInspectionApplicable: boolean
): QaItemDisplayInspectionType[] => {
  const inspectionTypes: QaItemDisplayInspectionType[] = []
  if (item.firstInspectionEnabled) {
    inspectionTypes.push('FIRST')
  }
  if (item.patrolInspectionEnabled) {
    inspectionTypes.push('PATROL')
  }
  if (finalInspectionApplicable) {
    inspectionTypes.push('FINAL')
  }
  return inspectionTypes
}
