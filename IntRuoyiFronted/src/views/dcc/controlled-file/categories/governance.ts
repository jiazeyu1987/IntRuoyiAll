import type {
  ControlledFileApprovalRoutePreviewVO
} from '@/api/dcc/controlledFile/approvalRoutes'
import type {
  ControlledFileCategoryApprovalMatrixVO,
  ControlledFileCategoryReviewMatrixRuleVO,
  ControlledFileCategoryReviewMatrixSubjectType,
  ControlledFileCategoryVO
} from '@/api/dcc/controlledFile/fileCategories'
import type { DccCategoryPermission } from '../shared/lifecycle'

export type CategoryPermissionSubjectType = 'USER' | 'DEPT' | 'ROLE' | 'POSITION'

export interface CategoryPermissionRuleDraft {
  localId: string
  actionType: DccCategoryPermission
  subjectType: CategoryPermissionSubjectType
  subjectId?: number
  active: boolean
  remark?: string
}

export interface CategoryDepartmentRuleDraft {
  localId: string
  departmentId?: number
  distributionMedium?: 'PUBLIC_FOLDER' | 'PAPER'
  active: boolean
}

type ReviewMatrixDraftSubjectType = 'USER' | 'DEPT' | 'ROLE' | 'POST' | 'DCC_POSITION'

export interface ReviewMatrixRuleDraft {
  stageType: 'SIGNOFF' | 'APPROVAL'
  active: boolean
  subjectLabel?: string
  marker: '▲'
  subjectType?: ReviewMatrixDraftSubjectType
  subjectId?: number
  subjectName?: string
  subjectDepartmentPath?: string
}

export interface ApprovalMatrixDraft {
  effectiveTime: string
  remark: string
  rules: ReviewMatrixRuleDraft[]
}

interface GovernanceServiceDeps {
  replacePermissionRules: (
    categoryId: number,
    data: ReturnType<typeof buildPermissionRulePayload>
  ) => Promise<unknown>
  saveCategoryApprovalMatrix: (
    categoryId: number,
    data: {
      effectiveTime: string
      remark?: string
      rules: ApprovalMatrixDraft['rules']
    }
  ) => Promise<unknown>
  replaceDistributionRules: (
    categoryId: number,
    data: ReturnType<typeof buildDepartmentRulePayload>
  ) => Promise<unknown>
  replaceTrainingRules: (
    categoryId: number,
    data: ReturnType<typeof buildDepartmentRulePayload>
  ) => Promise<unknown>
  previewApprovalRoute: (data: {
    categoryId: number
  }) => Promise<ControlledFileApprovalRoutePreviewVO[]>
}

let nextLocalId = 0

const createLocalId = (prefix: string) => {
  nextLocalId += 1
  return `${prefix}-${nextLocalId}`
}

const normalizeText = (value?: string) => {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

const normalizeReviewMatrixSubjectType = (
  subjectType?: ControlledFileCategoryReviewMatrixSubjectType
): ReviewMatrixDraftSubjectType | undefined => {
  const normalized = subjectType?.trim().toUpperCase()
  if (normalized === 'POSITION' || normalized === 'DCC_POSITION') {
    return 'DCC_POSITION'
  }
  if (
    normalized === 'USER' ||
    normalized === 'DEPT' ||
    normalized === 'ROLE' ||
    normalized === 'POST'
  ) {
    return normalized
  }
  return undefined
}

const normalizeReviewMatrixRule = (
  rule?: Partial<ControlledFileCategoryReviewMatrixRuleVO>
): ReviewMatrixRuleDraft => ({
  stageType: rule?.stageType === 'APPROVAL' ? 'APPROVAL' : 'SIGNOFF',
  active: rule?.active ?? true,
  subjectLabel: normalizeText(rule?.subjectLabel),
  marker: '▲',
  subjectType: normalizeReviewMatrixSubjectType(rule?.subjectType),
  subjectId: rule?.subjectId,
  subjectName: normalizeText(rule?.subjectName),
  subjectDepartmentPath: normalizeText(rule?.subjectDepartmentPath)
})

export const createPermissionRuleDraft = (
  overrides: Partial<CategoryPermissionRuleDraft> = {}
): CategoryPermissionRuleDraft => ({
  localId: overrides.localId ?? createLocalId('permission'),
  actionType: overrides.actionType ?? 'VIEW',
  subjectType: overrides.subjectType ?? 'DEPT',
  subjectId: overrides.subjectId,
  active: overrides.active ?? true,
  remark: overrides.remark ?? ''
})

export const createDepartmentRuleDraft = (
  overrides: Partial<CategoryDepartmentRuleDraft> = {}
): CategoryDepartmentRuleDraft => ({
  localId: overrides.localId ?? createLocalId('department'),
  departmentId: overrides.departmentId,
  distributionMedium: overrides.distributionMedium,
  active: overrides.active ?? true
})

export const createApprovalMatrixDraft = (
  matrix?: Partial<ControlledFileCategoryApprovalMatrixVO>
): ApprovalMatrixDraft => ({
  effectiveTime: matrix?.effectiveTime || '',
  remark: matrix?.remark || '',
  rules: [...(matrix?.rules || [])].map((rule) => normalizeReviewMatrixRule(rule))
})

export const buildPermissionRulePayload = (drafts: ReadonlyArray<CategoryPermissionRuleDraft>) => {
  return drafts.map((draft) => ({
    actionType: draft.actionType,
    subjectType: draft.subjectType,
    subjectId: draft.subjectId as number,
    active: draft.active,
    remark: draft.remark?.trim() ? draft.remark.trim() : undefined
  }))
}

export const buildDepartmentRulePayload = (drafts: ReadonlyArray<CategoryDepartmentRuleDraft>) => {
  return drafts.map((draft) => ({
    departmentId: draft.departmentId as number,
    ...(draft.distributionMedium ? { distributionMedium: draft.distributionMedium } : {}),
    active: draft.active
  }))
}

const buildApprovalMatrixPayload = (draft: ApprovalMatrixDraft) => ({
  effectiveTime: draft.effectiveTime,
  remark: draft.remark.trim() || undefined,
  rules: draft.rules.map((rule) => ({
    stageType: rule.stageType,
    active: rule.active,
    subjectLabel: normalizeText(rule.subjectLabel),
    marker: '▲' as const,
    subjectType: rule.subjectType,
    subjectId: rule.subjectId,
    subjectName: normalizeText(rule.subjectName),
    subjectDepartmentPath: normalizeText(rule.subjectDepartmentPath)
  }))
})

export const createGovernanceService = (deps: GovernanceServiceDeps) => {
  return {
    async savePermissionRules(
      category: Pick<ControlledFileCategoryVO, 'id'>,
      drafts: ReadonlyArray<CategoryPermissionRuleDraft>
    ) {
      await deps.replacePermissionRules(category.id as number, buildPermissionRulePayload(drafts))
    },
    async saveApprovalMatrix(
      category: Pick<ControlledFileCategoryVO, 'id'>,
      draft: ApprovalMatrixDraft
    ) {
      await deps.saveCategoryApprovalMatrix(category.id as number, buildApprovalMatrixPayload(draft))
      return await deps.previewApprovalRoute({ categoryId: category.id as number })
    },
    async saveDistributionRules(
      category: Pick<ControlledFileCategoryVO, 'id'>,
      drafts: ReadonlyArray<CategoryDepartmentRuleDraft>
    ) {
      await deps.replaceDistributionRules(category.id as number, buildDepartmentRulePayload(drafts))
    },
    async saveTrainingRules(
      category: Pick<ControlledFileCategoryVO, 'id'>,
      drafts: ReadonlyArray<CategoryDepartmentRuleDraft>
    ) {
      await deps.replaceTrainingRules(category.id as number, buildDepartmentRulePayload(drafts))
    }
  }
}