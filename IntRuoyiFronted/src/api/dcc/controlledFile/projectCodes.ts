import request from '@/config/axios'
import type { ControlledFileVO } from './workflow'

export const DCC_PROJECT_CODE_STATUS_ENABLE = 'ENABLE'
export const DCC_PROJECT_CODE_STATUS_DISABLE = 'DISABLE'

export interface DccProjectCodePageReqVO extends PageParam {
  productMasterId?: number | string
  keyword?: string
  projectName?: string
  projectCode?: string
  category?: string
  priority?: string
  status?: string
  routeConfigured?: boolean
  mainBatchRecordConfigured?: boolean
  qaRegulationConfigured?: boolean
  fileCountSort?: 'asc' | 'desc'
}

export interface DccProjectCodeRespVO {
  id: number
  productMasterId?: number | null
  docControlNo?: string | null
  projectName: string
  projectCode: string
  category?: string | null
  commissionedProduction?: string | null
  projectLeader?: string | null
  projectEngineer?: string | null
  storageLocation?: string | null
  priority?: string | null
  status: string
  associatedFileCount?: number | null
  createTime?: number
  updateTime?: number
}

export interface DccProjectCodeSaveReqVO {
  productMasterId?: number | null
  docControlNo?: string | null
  projectName: string
  projectCode?: string
  category?: string | null
  commissionedProduction?: string | null
  projectLeader?: string | null
  projectEngineer?: string | null
  storageLocation?: string | null
  priority?: string | null
  status: string
}

export interface DccProjectCodeUpdateReqVO extends DccProjectCodeSaveReqVO {
  id: number
}

export interface DccProjectCodeControlledFilePageReqVO extends PageParam {
  keyword?: string
  status?: string
}

export interface DccProjectCodeAssociatedFileAiCategoryRespVO {
  fileId: number
  fileName?: string | null
  currentStage?: string | null
  currentFileType?: string | null
  targetStage?: string | null
  targetFileType?: string | null
  matched: boolean
  classificationStatus?: 'MATCHED' | 'UNCLASSIFIED' | 'AMBIGUOUS'
  classificationMessage?: string | null
}

export interface DccProjectCodeImportRowRespVO {
  rowNo: number
  docControlNo?: string | null
  projectName?: string | null
  projectCode?: string | null
  category?: string | null
  commissionedProduction?: string | null
  projectLeader?: string | null
  projectEngineer?: string | null
  storageLocation?: string | null
  priority?: string | null
  currentStatus?: string | null
  importAction: string
  failureReason?: string | null
}

export interface DccProjectCodeImportPreviewRespVO {
  batchId: number
  status: string
  totalCount: number
  createCount: number
  updateCount: number
  disableCount: number
  unchangedCount: number
  failureCount: number
  rows: DccProjectCodeImportRowRespVO[]
}

export interface DccProductOnboardingCreateReqVO {
  productMasterId?: number | null
  productCode?: string | null
  dccProductCode?: string | null
  productNameCn?: string | null
  productNameEn?: string | null
  modelSpecification?: string | null
  productCategory?: string | null
  docControlNo?: string | null
  projectName: string
  projectCode: string
  category?: string | null
  commissionedProduction?: string | null
  projectLeader?: string | null
  projectEngineer?: string | null
  storageLocation?: string | null
  priority?: string | null
}

export interface DccProductOnboardingRespVO extends DccProductOnboardingCreateReqVO {
  id: number
  status: string
  applicantUserId?: number | null
  approverUserId?: number | null
  approvedTime?: number | null
  generatedProjectCodeId?: number | null
  rejectReason?: string | null
  createTime?: number
  updateTime?: number
}

interface UploadCommonResult<T> {
  data: T
}

export const getProjectCodePage = async (
  params: DccProjectCodePageReqVO
): Promise<PageResult<DccProjectCodeRespVO[]>> => {
  return await request.get({ url: "/dcc/project-codes/page", params })
}

export const getProjectCode = async (id: number | string): Promise<DccProjectCodeRespVO> => {
  return await request.get({ url: `/dcc/project-codes/${id}` })
}

export const createProjectCode = async (data: DccProjectCodeSaveReqVO): Promise<number> => {
  return await request.post({ url: '/dcc/project-codes/create', data })
}

export const updateProjectCode = async (data: DccProjectCodeUpdateReqVO): Promise<boolean> => {
  return await request.put({ url: '/dcc/project-codes/update', data })
}

export const deleteProjectCode = async (id: number): Promise<boolean> => {
  return await request.delete({ url: `/dcc/project-codes/delete?id=${id}` })
}

export const getProjectCodeControlledFilesPage = async (
  id: number | string,
  params: DccProjectCodeControlledFilePageReqVO
): Promise<PageResult<ControlledFileVO[]>> => {
  return await request.get({ url: `/dcc/project-codes/${id}/controlled-files/page`, params })
}

export const getProjectCodeAssociatedFileAiCategoryCandidates = async (
  id: number | string
): Promise<DccProjectCodeAssociatedFileAiCategoryRespVO[]> => {
  return await request.get({ url: `/dcc/project-codes/${id}/associated-files/ai-category-candidates` })
}

export const classifyProjectCodeAssociatedFileByAi = async (
  id: number | string,
  fileId: number | string
): Promise<DccProjectCodeAssociatedFileAiCategoryRespVO> => {
  return await request.post({ url: `/dcc/project-codes/${id}/associated-files/${fileId}/ai-category` })
}

export const exportProjectCodeExcel = async (params: DccProjectCodePageReqVO) => {
  return await request.download({ url: "/dcc/project-codes/export-excel", params })
}

export const getProjectCodeImportTemplate = async () => {
  return await request.download({ url: "/dcc/project-codes/import-template" })
}

export const importProjectCodePreview = async (
  file: File
): Promise<DccProjectCodeImportPreviewRespVO> => {
  const data = new FormData()
  data.append('file', file)
  const result = await request.upload<UploadCommonResult<DccProjectCodeImportPreviewRespVO>>({
    url: "/dcc/project-codes/import-preview",
    data
  })
  return result.data
}

export const importProjectCodeConfirm = async (
  batchId: number
): Promise<DccProjectCodeImportPreviewRespVO> => {
  return await request.post({ url: "/dcc/project-codes/import-confirm", data: { batchId } })
}

export const createProductOnboardingRequest = async (
  data: DccProductOnboardingCreateReqVO
): Promise<number> => {
  return await request.post({ url: '/dcc/product-onboarding-requests/create', data })
}

export const approveProductOnboardingRequest = async (
  id: number | string
): Promise<DccProductOnboardingRespVO> => {
  return await request.post({ url: `/dcc/product-onboarding-requests/${id}/approve` })
}
