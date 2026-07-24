import request from '@/config/axios'

export type DccCategoryLifecycleStage =
  | 'PLAN'
  | 'INPUT'
  | 'OUTPUT'
  | 'VERIFICATION'
  | 'VALIDATION'
  | 'TRANSFER'

export interface ControlledFileCategoryVO {
  id?: number
  parentId?: number | null
  code: string
  name: string
  lifecycleStage: DccCategoryLifecycleStage
  fileTypeTaxonomyId?: number | null
  directoryId?: number | null
  active: boolean
  sort: number
  source?: string
  remark?: string
  description?: string
  distributionRequired?: boolean
  trainingRequired?: boolean
  signoffPositionIds?: number[]
  approvalPositionIds?: number[]
  createTime?: Date | string
}

export interface ControlledFileCategoryDirectoryBindingReqVO {
  directoryId: number
  active: boolean
}

export type ControlledFileCategoryPermissionAction =
  | 'VIEW'
  | 'UPLOAD'
  | 'DOWNLOAD'
  | 'OBSOLETE'
  | 'REVIEW'
  | 'APPROVE'
  | 'DISTRIBUTE'

export type ControlledFileCategoryPermissionSubjectType = 'USER' | 'DEPT' | 'ROLE' | 'POSITION'

export interface ControlledFileCategoryPermissionRuleVO {
  id?: number
  categoryId?: number
  actionType: ControlledFileCategoryPermissionAction
  subjectType: ControlledFileCategoryPermissionSubjectType
  subjectId: number
  active: boolean
  remark?: string
}

export interface ControlledFileCategoryDepartmentRuleVO {
  id?: number
  categoryId?: number
  departmentId: number
  distributionMedium?: 'PUBLIC_FOLDER' | 'PAPER'
  active: boolean
}

export interface ControlledFileCategoryApprovalMatrixVO {
  categoryId: number
  routeVersionNo?: number
  effectiveTime?: string
  remark?: string
  rules: ControlledFileCategoryReviewMatrixRuleVO[]
}

export interface ControlledFileCategoryApprovalMatrixSaveReqVO {
  effectiveTime: string
  remark?: string
  rules: ControlledFileCategoryReviewMatrixRuleVO[]
}

export interface ControlledFileCategoryReviewMatrixQueryReqVO {
  code?: string
  name?: string
  active?: boolean
  configured?: boolean
}

export interface ControlledFileCategoryReviewMatrixRowVO {
  categoryId: number
  code: string
  name: string
  lifecycleStage: DccCategoryLifecycleStage
  active: boolean
  configured: boolean
  routeVersionNo?: number
  effectiveTime?: string
  remark?: string
  rules: ControlledFileCategoryReviewMatrixRuleVO[]
  viewRuleSummary?: string
  viewSubjects?: ControlledFileCategoryReviewMatrixSubjectVO[]
  pendingPreviewRuleSummary?: string
  downloadRuleSubjects?: string[]
  downloadRuleSummary?: string
  risks?: ControlledFileCategoryReviewMatrixRiskVO[]
}

export interface ControlledFileCategoryReviewMatrixSubjectVO {
  userId: number
  userName?: string
  source: string
  stageNo?: number
  stageName?: string
  stageType?: ControlledFileCategoryReviewMatrixStageType
  positionId?: number
  positionName?: string
  subjectLabel?: string
  marker?: '▲'
  subjectType?: ControlledFileCategoryReviewMatrixSubjectType
  subjectId?: number
  reason?: string
}

export interface ControlledFileCategoryReviewMatrixRiskVO {
  code: string
  message: string
  severity?: 'BLOCKING' | 'WARNING'
  blocking?: boolean
}

export interface ControlledFileCategoryReviewMatrixEffectivePreviewVO {
  categoryId: number
  nextRouteVersionNo?: number
  viewRuleSummary?: string
  pendingPreviewRuleSummary?: string
  downloadRuleSubjects?: string[]
  downloadRuleSummary?: string
  blocking?: boolean
  rules?: ControlledFileCategoryReviewMatrixRuleVO[]
  stages: ControlledFileCategoryReviewMatrixPreviewStageVO[]
  viewSubjects: ControlledFileCategoryReviewMatrixSubjectVO[]
  risks: ControlledFileCategoryReviewMatrixRiskVO[]
}

export interface ControlledFileCategoryReviewMatrixPreviewStageVO {
  stageNo: number
  stageName: string
  stageType?: ControlledFileCategoryReviewMatrixStageType
  approveMethod?: string
  positionIds?: number[]
  positionNames?: string[]
  sourceRule?: string
  resolvedSubjects?: ControlledFileCategoryReviewMatrixSubjectVO[]
}

export type ControlledFileCategoryReviewMatrixStageType = 'SIGNOFF' | 'APPROVAL'

export type ControlledFileCategoryReviewMatrixSubjectType =
  | 'USER'
  | 'DEPT'
  | 'ROLE'
  | 'POST'
  | 'POSITION'
  | 'DCC_POSITION'
  | 'UNMAPPED_EXCEL'

export interface ControlledFileCategoryReviewMatrixRuleVO {
  stageType: ControlledFileCategoryReviewMatrixStageType
  active: boolean
  subjectLabel?: string
  marker?: '▲'
  subjectType?: ControlledFileCategoryReviewMatrixSubjectType
  subjectId?: number
  subjectName?: string
  subjectDepartmentPath?: string
}

export type ControlledFileCapabilityStatus = 'YES' | 'NO' | 'CONDITIONAL'

export interface ControlledFileCategoryReviewMatrixUserLookupSourceVO {
  source: string
  reason?: string
  stageNo?: number
  stageName?: string
  positionId?: number
  positionName?: string
}

export interface ControlledFileCategoryReviewMatrixUserLookupVO {
  categoryId: number
  code: string
  name: string
  browseStatus: ControlledFileCapabilityStatus
  browseSource?: string
  browseReason?: string
  detailStatus: ControlledFileCapabilityStatus
  detailSource?: string
  detailReason?: string
  publishedPreviewStatus: ControlledFileCapabilityStatus
  publishedPreviewSource?: string
  publishedPreviewReason?: string
  pendingPreviewStatus: ControlledFileCapabilityStatus
  pendingPreviewSource?: string
  pendingPreviewReason?: string
  downloadStatus: ControlledFileCapabilityStatus
  downloadSource?: string
  downloadReason?: string
  viewSources?: ControlledFileCategoryReviewMatrixUserLookupSourceVO[]
  risks?: ControlledFileCategoryReviewMatrixRiskVO[]
}

export interface ControlledFileCategoryViewMatrixQueryReqVO {
  code?: string
  name?: string
  active?: boolean
  configured?: boolean
}

export type ControlledFileCategoryViewMatrixSubjectType =
  | 'USER'
  | 'DEPT'
  | 'POST'
  | 'POSITION'
  | 'ROLE'
  | 'DCC_POSITION'
  | 'UNMAPPED_EXCEL'

export type ControlledFileCategoryViewMatrixScopeType = 'ALL_MEMBERS' | 'MANAGER_AND_ABOVE'

export interface ControlledFileCategoryViewMatrixRuleVO {
  id?: number
  excelFileName?: string
  excelRowNo?: number
  excelColumnLetter?: string
  subjectLabel?: string
  subjectTopHeader?: string
  subjectSubHeader?: string
  marker?: '●' | '▲'
  scopeType?: ControlledFileCategoryViewMatrixScopeType
  subjectType?: ControlledFileCategoryViewMatrixSubjectType
  subjectId?: number
  subjectName?: string
  subjectDepartmentPath?: string
  active: boolean
  remark?: string
}

export interface ControlledFileCategoryViewMatrixSubjectVO {
  userId: number
  userName?: string
  source: 'CURRENT_VIEW_MATRIX' | string
  excelFileName?: string
  excelRowNo?: number
  excelColumnLetter?: string
  subjectLabel?: string
  marker?: '●' | '▲'
  scopeType?: ControlledFileCategoryViewMatrixScopeType
  subjectType?: ControlledFileCategoryViewMatrixSubjectType
  subjectId?: number
  reason?: string
}

export interface ControlledFileCategoryViewMatrixRiskVO {
  code: string
  message: string
  severity?: 'ERROR' | 'WARNING'
  blocking?: boolean
}

export interface ControlledFileCategoryViewMatrixRowVO {
  categoryId: number
  code: string
  name: string
  active: boolean
  sort: number
  configured: boolean
  viewRuleSummary?: string
  rules?: ControlledFileCategoryViewMatrixRuleVO[]
  viewSubjects?: ControlledFileCategoryViewMatrixSubjectVO[]
  pendingPreviewRuleSummary?: string
  downloadRuleSubjects?: string[]
  downloadRuleSummary?: string
  risks?: ControlledFileCategoryViewMatrixRiskVO[]
}

export interface ControlledFileCategoryViewMatrixSaveReqVO {
  rules: ControlledFileCategoryViewMatrixRuleVO[]
}

export interface ControlledFileCategoryViewMatrixEffectivePreviewVO {
  categoryId: number
  viewRuleSummary?: string
  pendingPreviewRuleSummary?: string
  downloadRuleSubjects?: string[]
  downloadRuleSummary?: string
  blocking?: boolean
  rules?: ControlledFileCategoryViewMatrixRuleVO[]
  viewSubjects?: ControlledFileCategoryViewMatrixSubjectVO[]
  risks?: ControlledFileCategoryViewMatrixRiskVO[]
}

export interface ControlledFileCategoryViewMatrixUserLookupVO {
  categoryId: number
  code: string
  name: string
  browseStatus: ControlledFileCapabilityStatus
  browseSource?: string
  browseReason?: string
  detailStatus: ControlledFileCapabilityStatus
  detailSource?: string
  detailReason?: string
  publishedPreviewStatus: ControlledFileCapabilityStatus
  publishedPreviewSource?: string
  publishedPreviewReason?: string
  pendingPreviewStatus: ControlledFileCapabilityStatus
  pendingPreviewSource?: string
  pendingPreviewReason?: string
  downloadStatus: ControlledFileCapabilityStatus
  downloadSource?: string
  downloadReason?: string
  viewSources?: ControlledFileCategoryViewMatrixSubjectVO[]
  risks?: ControlledFileCategoryViewMatrixRiskVO[]
}

export const getFileCategoryList = async (): Promise<ControlledFileCategoryVO[]> => {
  return await request.get({ url: '/dcc/file-categories' })
}

export const createFileCategory = async (data: ControlledFileCategoryVO) => {
  return await request.post({ url: '/dcc/file-categories', data })
}

export const updateFileCategory = async (id: number, data: ControlledFileCategoryVO) => {
  return await request.put({ url: `/dcc/file-categories/${id}`, data })
}

export const deleteFileCategory = async (id: number) => {
  return await request.delete({ url: `/dcc/file-categories/${id}` })
}

export const bindCategoryDirectory = async (
  id: number,
  data: ControlledFileCategoryDirectoryBindingReqVO
) => {
  return await request.put({ url: `/dcc/file-categories/${id}/directory-binding`, data })
}

export const getCategoryPermissionRules = async (
  id: number
): Promise<ControlledFileCategoryPermissionRuleVO[]> => {
  return await request.get({ url: `/dcc/file-categories/${id}/permission-rules` })
}

export const replaceCategoryPermissionRules = async (
  id: number,
  data: ControlledFileCategoryPermissionRuleVO[]
): Promise<ControlledFileCategoryPermissionRuleVO[]> => {
  return await request.put({ url: `/dcc/file-categories/${id}/permission-rules`, data })
}

export const getCategoryDistributionRules = async (
  id: number
): Promise<ControlledFileCategoryDepartmentRuleVO[]> => {
  return await request.get({ url: `/dcc/file-categories/${id}/distribution-rules` })
}

export const replaceCategoryDistributionRules = async (
  id: number,
  data: ControlledFileCategoryDepartmentRuleVO[]
): Promise<ControlledFileCategoryDepartmentRuleVO[]> => {
  return await request.put({ url: `/dcc/file-categories/${id}/distribution-rules`, data })
}

export const getCategoryTrainingRules = async (
  id: number
): Promise<ControlledFileCategoryDepartmentRuleVO[]> => {
  return await request.get({ url: `/dcc/file-categories/${id}/training-rules` })
}

export const replaceCategoryTrainingRules = async (
  id: number,
  data: ControlledFileCategoryDepartmentRuleVO[]
): Promise<ControlledFileCategoryDepartmentRuleVO[]> => {
  return await request.put({ url: `/dcc/file-categories/${id}/training-rules`, data })
}

export const getCategoryApprovalMatrix = async (
  id: number
): Promise<ControlledFileCategoryApprovalMatrixVO> => {
  return await request.get({ url: `/dcc/file-categories/${id}/matrix` })
}

export const getCategoryReviewMatrixRows = async (
  params: ControlledFileCategoryReviewMatrixQueryReqVO
): Promise<ControlledFileCategoryReviewMatrixRowVO[]> => {
  return await request.get({ url: '/dcc/file-categories/review-matrix', params })
}

export const getCategoryViewMatrixRows = async (
  params: ControlledFileCategoryViewMatrixQueryReqVO
): Promise<ControlledFileCategoryViewMatrixRowVO[]> => {
  return await request.get({ url: '/dcc/file-categories/view-matrix', params })
}

export const saveCategoryApprovalMatrix = async (
  id: number,
  data: ControlledFileCategoryApprovalMatrixSaveReqVO
): Promise<number> => {
  return await request.put({ url: `/dcc/file-categories/${id}/matrix`, data })
}

export const previewCategoryApprovalMatrixEffectiveAccess = async (
  id: number,
  data: ControlledFileCategoryApprovalMatrixSaveReqVO
): Promise<ControlledFileCategoryReviewMatrixEffectivePreviewVO> => {
  return await request.post({ url: `/dcc/file-categories/${id}/matrix/effective-preview`, data })
}

export const getReviewMatrixUserLookup = async (
  userId: number
): Promise<ControlledFileCategoryReviewMatrixUserLookupVO[]> => {
  return await request.get({ url: '/dcc/file-categories/review-matrix/user-lookup', params: { userId } })
}

export const previewCategoryViewMatrixEffectiveAccess = async (
  id: number,
  data: ControlledFileCategoryViewMatrixSaveReqVO
): Promise<ControlledFileCategoryViewMatrixEffectivePreviewVO> => {
  return await request.post({ url: `/dcc/file-categories/${id}/view-matrix/effective-preview`, data })
}

export const saveCategoryViewMatrix = async (
  id: number,
  data: ControlledFileCategoryViewMatrixSaveReqVO
): Promise<ControlledFileCategoryViewMatrixRuleVO[]> => {
  return await request.put({ url: `/dcc/file-categories/${id}/view-matrix`, data })
}

export const getViewMatrixUserLookup = async (
  userId: number
): Promise<ControlledFileCategoryViewMatrixUserLookupVO[]> => {
  return await request.get({ url: '/dcc/file-categories/view-matrix/user-lookup', params: { userId } })
}

export const deleteCategoryApprovalMatrix = async (id: number): Promise<boolean> => {
  return await request.delete({ url: `/dcc/file-categories/${id}/matrix` })
}
