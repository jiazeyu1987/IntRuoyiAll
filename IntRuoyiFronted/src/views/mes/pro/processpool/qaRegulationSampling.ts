export type QaInspectionTypeValue = 'FIRST' | 'PATROL_AM' | 'PATROL_PM' | 'FINAL'

export interface QaInspectionSamplingPlan {
  firstInspectionQuantity?: number
  patrolAql: number
  patrolInspectionRatio: number
}

const FIRST_INSPECTION_MARKER_PATTERN = /(?:首件|首检)/
const FIRST_INSPECTION_QUANTITY_PATTERN = /(?:首件|首检)\s*[：:]\s*(\d+)\s*件?/
const PATROL_AQL_PATTERN = /AQL\s*[=＝]\s*(\d+(?:\.\d+)?)/i

const normalizeSamplingPlanText = (samplingPlanText?: string) => samplingPlanText?.trim() ?? ''

const resolveFirstInspectionQuantity = (samplingPlanText?: string) => {
  const match = normalizeSamplingPlanText(samplingPlanText).match(
    FIRST_INSPECTION_QUANTITY_PATTERN
  )
  if (!match) {
    return undefined
  }
  const quantity = Number(match[1])
  return Number.isSafeInteger(quantity) && quantity > 0 ? quantity : undefined
}

const resolvePatrolAql = (samplingPlanText?: string) => {
  const match = normalizeSamplingPlanText(samplingPlanText).match(PATROL_AQL_PATTERN)
  if (!match) {
    return undefined
  }
  const patrolAql = Number(match[1])
  return Number.isFinite(patrolAql) && patrolAql > 0 && patrolAql <= 100
    ? patrolAql
    : undefined
}

export const resolveQaApplicableInspectionTypes = (
  samplingPlanText: string | undefined,
  finalInspectionRequired: boolean
): QaInspectionTypeValue[] => {
  const inspectionTypes: QaInspectionTypeValue[] = ['PATROL_AM', 'PATROL_PM']
  if (resolveFirstInspectionQuantity(samplingPlanText)) {
    inspectionTypes.unshift('FIRST')
  }
  if (finalInspectionRequired) {
    inspectionTypes.push('FINAL')
  }
  return inspectionTypes
}

export const isQaInspectionSamplingPlanComplete = (samplingPlanText?: string) => {
  const normalizedText = normalizeSamplingPlanText(samplingPlanText)
  const firstInspectionQuantity = resolveFirstInspectionQuantity(normalizedText)
  const firstInspectionQuantityValid =
    !FIRST_INSPECTION_MARKER_PATTERN.test(normalizedText) || Boolean(firstInspectionQuantity)
  return firstInspectionQuantityValid && Boolean(resolvePatrolAql(normalizedText))
}

export const parseQaInspectionSamplingPlan = (
  samplingPlanText: string | undefined,
  itemName: string
): QaInspectionSamplingPlan => {
  const normalizedText = normalizeSamplingPlanText(samplingPlanText)
  const firstInspectionQuantity = resolveFirstInspectionQuantity(normalizedText)
  if (FIRST_INSPECTION_MARKER_PATTERN.test(normalizedText) && !firstInspectionQuantity) {
    throw new Error(`${itemName}抽样方案中的首检数量必须是大于 0 的整数`)
  }
  const patrolAql = resolvePatrolAql(normalizedText)
  if (!patrolAql) {
    throw new Error(`${itemName}抽样方案必须包含 0 到 100 之间的有效巡检 AQL 抽样比例`)
  }
  return {
    firstInspectionQuantity,
    patrolAql,
    patrolInspectionRatio: patrolAql
  }
}
