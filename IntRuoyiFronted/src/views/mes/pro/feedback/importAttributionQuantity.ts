import type { ProFeedbackImportCandidateVO } from '@/api/mes/pro/feedback'

export const INFINITE_PLANNED_QUANTITY_SENTINEL = 999999
export const INFINITE_PLANNED_QUANTITY_LABEL = '无限大'

const toFiniteQuantity = (value?: number) => {
  const normalized = Number(value ?? 0)
  return Number.isFinite(normalized) ? normalized : 0
}

export const isInfinitePlannedQuantity = (value?: number) =>
  toFiniteQuantity(value) === INFINITE_PLANNED_QUANTITY_SENTINEL

export const formatCandidatePlannedQuantity = (value?: number) =>
  isInfinitePlannedQuantity(value) ? INFINITE_PLANNED_QUANTITY_LABEL : value

export const resolveCurrentOrderFillQuantity = (candidate?: ProFeedbackImportCandidateVO) => {
  const plannedQuantity = toFiniteQuantity(candidate?.plannedQuantity)
  const remainingQuantity = toFiniteQuantity(candidate?.remainingQuantity)
  if (plannedQuantity <= 0 || remainingQuantity <= 0) {
    return 0
  }
  return Math.min(plannedQuantity, remainingQuantity)
}
