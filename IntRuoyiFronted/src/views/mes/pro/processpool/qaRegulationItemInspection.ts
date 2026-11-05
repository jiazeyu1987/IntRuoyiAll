export type QaItemDisplayInspectionType = 'FIRST' | 'PATROL' | 'FINAL'
export type QaItemPersistedInspectionType = 'FIRST' | 'PATROL' | 'FINAL'

export interface QaItemInspectionSource {
  applicableInspectionTypes: readonly string[]
  firstInspectionQuantity?: number
  finalInspectionQuantity?: number
  patrolInspectionRatio?: number
}

export interface QaItemInspectionState {
  finalInspectionEnabled: boolean
  firstInspectionEnabled: boolean
  firstInspectionQuantity?: number
  finalInspectionQuantity?: number
  patrolInspectionEnabled: boolean
  patrolInspectionRatio?: number
}

export interface QaItemInspectionPayload {
  applicableInspectionTypes: QaItemPersistedInspectionType[]
  firstInspectionQuantity?: number
  finalInspectionQuantity?: number
  patrolInspectionRatio?: number
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
    patrolInspectionRatio: source.patrolInspectionRatio
  }
}

const requireInspectionQuantity = (value: number | undefined, itemName: string, label: string) => {
  if (!Number.isSafeInteger(Number(value)) || Number(value) <= 0) {
    throw new Error(`${itemName}的${label}数量必须是大于 0 的整数`)
  }
  return Number(value)
}

const requirePatrolInspectionRatio = (value: number | undefined, itemName: string) => {
  if (!Number.isFinite(Number(value)) || Number(value) <= 0 || Number(value) > 100) {
    throw new Error(`${itemName}的巡检比例必须大于 0 且不超过 100%`)
  }
  return Number(value)
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
  const patrolInspectionRatio = item.patrolInspectionEnabled
    ? requirePatrolInspectionRatio(item.patrolInspectionRatio, itemName)
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
    patrolInspectionRatio
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
    (Number.isFinite(Number(item.patrolInspectionRatio)) &&
      Number(item.patrolInspectionRatio) > 0 &&
      Number(item.patrolInspectionRatio) <= 100)) &&
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
