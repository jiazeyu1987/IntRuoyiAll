import request from '@/config/axios'

const SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT = 5 * 60 * 1000
const SHOWROOM_RELEASE_PUBLISH_REQUEST_TIMEOUT = 0
const SHOWROOM_PRODUCT_IMPORT_REQUEST_TIMEOUT = 5 * 60 * 1000
const SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_TIMEOUT = 5 * 60 * 1000
export const SHOWROOM_ANDROID_CLIENT_DOWNLOAD_URL = '/showroom/client-downloads/android'
export const SHOWROOM_ANDROID_CLIENT_FILE_NAME = 'YingtaiShowroomClient-Android-v1.0.apk'

export interface ShowroomSubmitReqVO {
  targetId: number
  targetRevisionId?: number
  fieldCodes: string[]
  moduleCode?: string
  submittedBy?: number
  submitterDeptId?: number | null
  supervisorUserId?: number | null
}

export interface ShowroomPageQuery extends PageParam {
  productId?: number
  keyword?: string
  lifecycleStage?: string
  incompleteStatus?: string
  approvalStatus?: string
}

export interface ShowroomKeywordPageReqVO extends PageParam {
  keyword?: string
}

export interface ShowroomKeywordPageRowRespVO {
  id: number
  nameZh: string
  nameEn: string
  updateTime?: string | number | null
}

export interface ShowroomKeywordRespVO {
  id: number
  nameZh: string
  nameEn: string
  createTime?: string | number | null
  updateTime?: string | number | null
}

export interface ShowroomKeywordSaveReqVO {
  id?: number
  nameZh: string
  nameEn: string
}

export interface ShowroomHallMappingReqVO {
  hallId: number
  items: Array<{
    itemType: 'PRODUCT' | 'AWARD' | string
    itemId: number
    displayOrder: number
    layoutX?: number
    layoutY?: number
    layoutWidth?: number
    layoutHeight?: number
  }>
}

export interface ShowroomHallCanvasLayoutReqVO {
  hallId: number
  items: Array<{
    itemType: 'PRODUCT' | 'AWARD' | string
    itemId: number
    displayOrder: number
    layoutX: number
    layoutY: number
    layoutWidth: number
    layoutHeight: number
  }>
}

export interface ShowroomHallCanvasBackgroundReqVO {
  hallId: number
  canvasBackgroundImageUrl: string
}

export interface ShowroomHallPreviewAssetPublishReqVO {
  hallId: number
  imageFileId: number
}

export interface ShowroomHallPreviewAssetPublishRespVO {
  hallId: number
  previewAssetVersionId: number
  imageFileId: number
  previewImageUrl: string
  live: boolean
}

export interface ShowroomHallConfigPackageImportRespVO {
  hallCount: number
  keywordCount: number
  previewAssetCount: number
  narrationCount: number
  backgroundAssetCount: number
  removedHallCount: number
  removedKeywordCount: number
  validatedProductCount: number
  validatedAwardCount: number
}

export interface ShowroomHallProductOptionRespVO {
  itemType?: 'PRODUCT' | 'AWARD' | string
  productId: number
  productMasterId?: number
  productCode: string
  nameCn: string
  revisionNo: number
  incomplete: boolean
  previewImageUrl: string
  hallIds: number[]
}

export interface ShowroomAwardPageRowRespVO {
  awardId: number
  awardCode: string
  nameCn: string
  nameEn: string
  issuer: string
  awardDateText: string
  coverImageUrl: string
  incomplete: boolean
  revisionNo: number
}

export interface ShowroomAwardImportFailureRespVO {
  rowNo: number
  awardCode: string
  reason: string
}

export interface ShowroomAwardCoverGenerateRespVO {
  awardId: number
  revisionId: number
  revisionNo: number
  coverImageUrl: string
}

export interface ShowroomImagePromptCurrentRespVO {
  promptVersionId: number
  sceneCode: 'PRODUCT_COVER' | string
  versionNo: number
  templateText: string
  changeNote: string
  placeholderCodes: string[]
  useCount: number
  createTime?: number | null
  creator: string
  lastUsedAt?: number | null
}

export interface ShowroomImagePromptHistoryItemRespVO extends ShowroomImagePromptCurrentRespVO {
  current: boolean
}

export interface ShowroomImagePromptVersionSaveReqVO {
  sceneCode: 'PRODUCT_COVER' | string
  templateText: string
  changeNote?: string
}

export interface ShowroomNarrationTtsDefaultsRespVO {
  defaultVoice: string
  voiceSaved: boolean
  voiceConfigured: boolean
  voiceSource: 'saved' | 'runtime' | 'missing' | string
  appKeySaved: boolean
  appKeyConfigured: boolean
  appKeySource: 'saved' | 'runtime' | 'missing' | string
  maskedAppKey?: string
  tokenSaved: boolean
  tokenConfigured: boolean
  tokenSource: 'saved' | 'runtime' | 'missing' | string
  maskedAccessToken?: string
}

export interface ShowroomProductNarrationGenerateReqVO {
  productId: number
  sourceRevisionId?: number | null
}

export interface ShowroomHallNarrationGenerateReqVO {
  hallId: number
}

export interface ShowroomHallNarrationGenerateRespVO {
  hallId: number
  zhNarrationVersionId: number
  enNarrationVersionId: number
  voice: string
}

export interface ShowroomHallNarrationBatchGenerateFailureRespVO {
  hallId: number
  hallCode: string
  name: string
  reason: string
}

export interface ShowroomHallNarrationBatchGenerateRespVO {
  matchedCount: number
  succeededCount: number
  failedCount: number
  failures: ShowroomHallNarrationBatchGenerateFailureRespVO[]
}

export interface ShowroomProductFieldTranslateReqVO {
  productId: number
  nameCn: string
  fields: Record<string, string>
  narrationScriptZh?: string | null
}

export interface ShowroomProductFieldTranslateRespVO {
  productId: number
  nameEn: string
  translatedFields: Record<string, string>
  narrationScriptEn?: string | null
}

export type ShowroomProductAttachmentAssetType = 'image' | 'video' | 'text'

export interface ShowroomProductAttachment {
  id?: number | null
  assetType: ShowroomProductAttachmentAssetType
  fileId: number
  url?: string
  originalName: string
  mimeType: string
  size: number
  displayOrder: number
}

export interface ShowroomProductAttachmentUploadRespVO {
  fileId: number
  url: string
  originalName: string
  mimeType: string
  size: number
  assetType: ShowroomProductAttachmentAssetType
}

export interface ShowroomProductPublishReqVO {
  productId?: number
  productMasterId?: number
  productCode: string
  legacyProductCode?: string
  nameCn: string
  nameEn: string
  fields: Record<string, string>
  sourceRevisionId?: number | null
  narrationScriptText?: string | null
  narrationGeneratedByAi: boolean
  attachments?: ShowroomProductAttachment[]
}

export interface ShowroomProductMaterialBlockerRespVO {
  blockerCode: string
  message: string
  targetType?: 'PRODUCT' | 'COMPANY' | 'HALL' | 'RELEASE' | string
  targetId?: number | null
  revisionId?: number | null
  language?: 'ZH' | 'EN' | string | null
  missingFields?: string[]
  fileId?: number | null
  assetId?: string | null
  contentHash?: string | null
}

export interface ShowroomProductPublishRespVO {
  productId: number
  revisionId: number
  revisionNo: number
  status: 'PUBLISHED'
  fields: Record<string, string>
  materialBlockers: ShowroomProductMaterialBlockerRespVO[]
}

export type ShowroomProductCoverGenerationMode = 'ALL' | 'MISSING_ONLY'

export interface ShowroomProductBatchGenerateReqVO {
  keyword?: string
  lifecycleStage?: string
  incompleteStatus?: string
  approvalStatus?: string
  coverGenerationMode?: ShowroomProductCoverGenerationMode
}

export interface ShowroomProductBatchGenerateFailureRespVO {
  productId: number
  productCode: string
  nameCn: string
  reason: string
}

export interface ShowroomProductSalesCountriesBatchGenerateRespVO {
  matchedCount: number
  skippedCompletedCount: number
  updatedProductCount: number
  generatedLanguageCount: number
  failedCount: number
  failures: ShowroomProductBatchGenerateFailureRespVO[]
}

export interface ShowroomProductBatchGenerateRespVO {
  matchedCount: number
  publishedCount: number
  skippedUnpublishedCount: number
  skippedExistingCount: number
  skippedMissingScriptCount: number
  succeededCount: number
  failedCount: number
  autoCheckEnabled: boolean
  remainingActionableCount: number
  taskId?: number | null
  taskStatus?: 'WAITING' | 'RUNNING' | 'COMPLETED' | string
  remainingPendingCount?: number
  nextCheckAt?: string | null
  failures: ShowroomProductBatchGenerateFailureRespVO[]
}

export interface ShowroomProductBatchGenerateStateRespVO {
  enabled: boolean
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
  matchedCount: number
  publishedCount: number
  skippedUnpublishedCount: number
  skippedExistingCount: number
  skippedMissingScriptCount: number
  succeededCount: number
  failedCount: number
  remainingActionableCount: number
  lastRunAt?: number | null
  lastFailureMessage?: string | null
  lastFailureAt?: number | null
}

export interface ShowroomProductBatchTaskCurrentProductRespVO {
  productId: number
  productCode: string
  nameCn: string
}

export interface ShowroomProductTranslatePublishBatchTaskRespVO {
  active: boolean
  running: boolean
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
  matchedCount: number
  succeededCount: number
  failedCount: number
  remainingCount: number
  startedAt?: number | null
  lastRunAt?: number | null
  completedAt?: number | null
  currentProduct?: ShowroomProductBatchTaskCurrentProductRespVO | null
  lastFailure?: ShowroomProductBatchGenerateFailureRespVO | null
  lastFailureAt?: number | null
  failures: ShowroomProductBatchGenerateFailureRespVO[]
}

export interface ShowroomProductNarrationScriptTaskCurrentProductRespVO {
  productId: number
  productCode: string
  nameCn: string
}

export interface ShowroomProductNarrationScriptTaskRespVO {
  active: boolean
  running: boolean
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
  matchedCount: number
  skippedCompletedCount: number
  generatedLanguageCount: number
  failedCount: number
  remainingCount: number
  startedAt?: number | null
  lastRunAt?: number | null
  completedAt?: number | null
  currentProduct?: ShowroomProductNarrationScriptTaskCurrentProductRespVO | null
  lastFailure?: ShowroomProductBatchGenerateFailureRespVO | null
  lastFailureAt?: number | null
}

export interface ShowroomProductCoverBatchTaskStateRespVO {
  startAllowed: boolean
  active: boolean
  running: boolean
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
  matchedCount: number
  publishedCount: number
  skippedUnpublishedCount: number
  skippedExistingCount: number
  succeededCount: number
  failedCount: number
  remainingPendingCount: number
  taskId?: number | null
  taskStatus?: 'WAITING' | 'RUNNING' | 'COMPLETED' | string
  nextCheckAt?: string | null
  lastRunAt?: number | null
  completedAt?: number | null
  lastFailureMessage?: string | null
  currentProduct?: ShowroomProductBatchTaskCurrentProductRespVO | null
}

export interface ShowroomProductImportFailureRespVO {
  rowNo: number
  productCode: string
  reason: string
}

export interface ShowroomProductImportRespVO {
  totalRows: number
  successCount: number
  skippedCount: number
  failureCount: number
  successProductCodes: string[]
  skippedProductCodes: string[]
  failures: ShowroomProductImportFailureRespVO[]
  awardTotalRows?: number
  awardSuccessCount?: number
  awardFailureCount?: number
  awardWarnings?: string[]
  successAwardCodes?: string[]
  awardFailures?: ShowroomAwardImportFailureRespVO[]
}

export type ShowroomProductImportMode = 'STANDARD' | 'BASE_WORKBOOK'

export interface ShowroomProductCoverGenerateReqVO {
  productId: number
  productCode: string
  nameCn: string
  nameEn: string
  fields: Record<string, string>
}

export interface ShowroomProductCoverGenerateRespVO {
  productId: number
  coverImage: string
  fileId?: number
  promptVersionId: number
  status?: 'DRAFT'
}

export interface ShowroomApprovalActionReqVO {
  id: number
  reviewerUserId: number
  password: string
  comment?: string
}

export interface ShowroomApprovalRejectReqVO {
  id: number
  reviewerUserId: number
  password: string
  reason: string
}

export interface ShowroomCompanyNarrationGenerateReqVO {
  companyId: number
  sourceRevisionId: number
  language: 'ZH' | 'EN'
  scriptText: string
}

export interface ShowroomCompanyNarrationScriptGenerateReqVO {
  companyId: number
  sourceRevisionId: number
  companyType: string
  displayName: string
  fields: Record<string, string>
  targetLength: number
}

export interface ShowroomCompanyNarrationScriptGenerateRespVO {
  companyId: number
  sourceRevisionId: number
  introTextZh: string
}

export interface ShowroomCompanyFieldTranslateReqVO {
  companyId: number
  fieldCodes: string[]
  fields: Record<string, string>
  introTextZh: string
}

export interface ShowroomCompanyFieldTranslateRespVO {
  companyId: number
  translatedFields: Record<string, string>
  introTextEn: string
}

export interface ShowroomCompanyNarrationVersionRespVO {
  narrationVersionId: number
  language: 'ZH' | 'EN'
  scriptText: string
  audioFileId: number
  audioDurationSeconds: number
  audioUrl: string
  voice: string
}

export interface ShowroomCompanyNarrationGenerateRespVO {
  companyId: number
  sourceRevisionId: number
  scriptText: string
  narration: ShowroomCompanyNarrationVersionRespVO
  voice: string
}

export interface ShowroomCompanyNarrationPublishReqVO {
  zhNarrationVersionId?: number | null
  enNarrationVersionId?: number | null
}

export interface ShowroomCompanyNarrationPublishRespVO {
  companyId: number
  zhNarrationVersionId?: number | null
  enNarrationVersionId?: number | null
}

export interface ShowroomCompanyRevisionRestoreReqVO {
  companyId: number
  sourceRevisionId: number
}

export interface ShowroomReleasePublishRespVO {
  releaseId: string
  manifestHash: string
  rootDocumentId: string
  documentCount: number
  assetCount: number
  installBytes: number
  publishedAt: string
}

export interface ShowroomReleasePublishReqVO {
  siteKey: string
  stage: 'TEST' | 'PROD'
}

export const ShowroomAdminApi = {
  getCompanyCurrent: async () => {
    return await request.get({ url: '/showroom/company/current' })
  },
  getCompany: async (id: number, revisionId?: number | null) => {
    return await request.get({
      url: '/showroom/company/get',
      params: revisionId ? { id, revisionId } : { id }
    })
  },
  downloadAndroidClient: async () => {
    return await request.download({ url: SHOWROOM_ANDROID_CLIENT_DOWNLOAD_URL })
  },
  generateCompanyNarrationScript: async (
    data: ShowroomCompanyNarrationScriptGenerateReqVO
  ): Promise<ShowroomCompanyNarrationScriptGenerateRespVO> => {
    return await request.post({
      url: '/showroom/company/generate-narration-script',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  translateCompanyFieldsToEn: async (
    data: ShowroomCompanyFieldTranslateReqVO
  ): Promise<ShowroomCompanyFieldTranslateRespVO> => {
    return await request.post({
      url: '/showroom/company/translate-fields-to-en',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  generateCompanyNarrationAudio: async (
    data: ShowroomCompanyNarrationGenerateReqVO
  ): Promise<ShowroomCompanyNarrationGenerateRespVO> => {
    return await request.post({
      url: '/showroom/company/generate-narration-audio',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  publishCompanyNarration: async (
    data: ShowroomCompanyNarrationPublishReqVO
  ): Promise<ShowroomCompanyNarrationPublishRespVO> => {
    return await request.post({ url: '/showroom/company/publish-narration', data })
  },
  publishCompany: async (data: any) => {
    return await request.put({ url: '/showroom/company/publish', data })
  },
  restoreCompanyRevision: async (data: ShowroomCompanyRevisionRestoreReqVO) => {
    return await request.post({ url: '/showroom/company/restore', data })
  },
  saveCompanyDraft: async (data: any) => {
    return await request.put({ url: '/showroom/company/draft', data })
  },
  submitCompany: async (data: ShowroomSubmitReqVO) => {
    return await request.post({ url: '/showroom/company/submit', data })
  },
  publishRelease: async (data: ShowroomReleasePublishReqVO): Promise<ShowroomReleasePublishRespVO> => {
    return await request.post({
      url: '/showroom/release/publish',
      data,
      timeout: SHOWROOM_RELEASE_PUBLISH_REQUEST_TIMEOUT
    })
  },
  getCompanyHistory: async (params?: any) => {
    return await request.get({ url: '/showroom/company/history', params })
  },
  getImagePromptCurrent: async (
    sceneCode: 'PRODUCT_COVER' | string
  ): Promise<ShowroomImagePromptCurrentRespVO> => {
    return await request.get({ url: '/showroom/prompt/current', params: { sceneCode } })
  },
  getImagePromptHistory: async (
    sceneCode: 'PRODUCT_COVER' | string
  ): Promise<ShowroomImagePromptHistoryItemRespVO[]> => {
    return await request.get({ url: '/showroom/prompt/history', params: { sceneCode } })
  },
  saveImagePromptVersion: async (
    data: ShowroomImagePromptVersionSaveReqVO
  ): Promise<ShowroomImagePromptCurrentRespVO> => {
    return await request.post({ url: '/showroom/prompt/version', data })
  },
  getProductPage: async (params: ShowroomPageQuery): Promise<PageResult<unknown[]>> => {
    return await request.get({ url: '/showroom/product/page', params })
  },
  getAwardPage: async (params: ShowroomPageQuery): Promise<PageResult<ShowroomAwardPageRowRespVO[]>> => {
    return await request.get({ url: '/showroom/award/page', params })
  },
  getKeywordPage: async (params: ShowroomKeywordPageReqVO): Promise<PageResult<ShowroomKeywordPageRowRespVO[]>> => {
    return await request.get({ url: '/showroom/keyword/page', params })
  },
  getKeyword: async (id: number): Promise<ShowroomKeywordRespVO> => {
    return await request.get({ url: '/showroom/keyword/get', params: { id } })
  },
  createKeyword: async (data: ShowroomKeywordSaveReqVO) => {
    return await request.post({ url: '/showroom/keyword/create', data })
  },
  updateKeyword: async (data: ShowroomKeywordSaveReqVO) => {
    return await request.put({ url: '/showroom/keyword/update', data })
  },
  deleteKeyword: async (id: number) => {
    return await request.delete({ url: `/showroom/keyword/delete?id=${id}` })
  },
  getAward: async (id: number) => {
    return await request.get({ url: '/showroom/award/get', params: { id } })
  },
  saveAwardDraft: async (data: any) => {
    return await request.put({ url: '/showroom/award/draft', data })
  },
  publishAward: async (data: {
    awardId: number
    revisionId: number
    awardCode: string
    nameCn: string
    nameEn: string
    descriptionZh: string
    descriptionEn: string
    issuer: string
    awardDateText: string
    coverImage: string
  }) => {
    return await request.put({ url: '/showroom/award/publish', data })
  },
  generateAwardCoverImage: async (data: { awardId: number }): Promise<ShowroomAwardCoverGenerateRespVO> => {
    return await request.post({
      url: '/showroom/award/generate-cover-image',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  deleteAward: async (id: number) => {
    return await request.delete({ url: `/showroom/award/delete?id=${id}` })
  },
  exportProductExcel: async (params: ShowroomPageQuery) => {
    return await request.download({
      url: '/showroom/product/export-excel',
      params,
      timeout: SHOWROOM_PRODUCT_RESOURCE_PACKAGE_EXPORT_TIMEOUT
    })
  },
  getProductImportTemplate: async () => {
    return await request.download({ url: '/showroom/product/get-import-template' })
  },
  importProductExcel: async (data: FormData): Promise<ShowroomProductImportRespVO> => {
    return await request.upload({
      url: '/showroom/product/import-excel',
      data,
      timeout: SHOWROOM_PRODUCT_IMPORT_REQUEST_TIMEOUT
    })
  },
  importProductBaseWorkbook: async (data: FormData): Promise<ShowroomProductImportRespVO> => {
    return await request.upload({
      url: '/showroom/product/import-base-workbook',
      data,
      timeout: SHOWROOM_PRODUCT_IMPORT_REQUEST_TIMEOUT
    })
  },
  createProduct: async (data: any) => {
    return await request.post({ url: '/showroom/product/create', data })
  },
  getProduct: async (id: number, revisionId?: number | null) => {
    return await request.get({
      url: '/showroom/product/get',
      params: revisionId ? { id, revisionId } : { id }
    })
  },
  saveProductDraft: async (data: any) => {
    return await request.put({ url: '/showroom/product/draft', data })
  },
  uploadProductAttachment: async (
    data: FormData
  ): Promise<ShowroomProductAttachmentUploadRespVO> => {
    const response = await request.upload<{ data: ShowroomProductAttachmentUploadRespVO }>({
      url: '/showroom/product/attachment/upload',
      data
    })
    return response.data as ShowroomProductAttachmentUploadRespVO
  },
  translateProductFieldsToEn: async (
    data: ShowroomProductFieldTranslateReqVO
  ): Promise<ShowroomProductFieldTranslateRespVO> => {
    return await request.post({
      url: '/showroom/product/translate-fields-to-en',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  publishProduct: async (data: ShowroomProductPublishReqVO): Promise<ShowroomProductPublishRespVO> => {
    return await request.put({ url: '/showroom/product/publish', data })
  },
  deleteProduct: async (id: number) => {
    return await request.delete({ url: `/showroom/product/delete?id=${id}` })
  },
  submitProduct: async (data: ShowroomSubmitReqVO) => {
    return await request.post({ url: '/showroom/product/submit', data })
  },
  getProductHistory: async (id: number) => {
    return await request.get({ url: `/showroom/product/history?id=${id}` })
  },
  generateProductNarrationScript: async (data: ShowroomProductNarrationGenerateReqVO) => {
    return await request.post({
      url: '/showroom/product/generate-narration-script',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  generateProductNarrationAudio: async (data: ShowroomProductNarrationGenerateReqVO) => {
    return await request.post({
      url: '/showroom/product/generate-narration-audio',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  batchPublishProducts: async (
    data: ShowroomProductBatchGenerateReqVO
  ): Promise<ShowroomProductBatchGenerateRespVO> => {
    return await request.post({
      url: '/showroom/product/batch-publish',
      data
    })
  },
  batchGenerateProductSalesCountries: async (
    data: ShowroomProductBatchGenerateReqVO
  ): Promise<ShowroomProductSalesCountriesBatchGenerateRespVO> => {
    return await request.post({
      url: '/showroom/product/batch-generate-sales-countries',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  batchGenerateProductNarrationAudio: async (
    data: ShowroomProductBatchGenerateReqVO
  ): Promise<ShowroomProductBatchGenerateRespVO> => {
    return await request.post({
      url: '/showroom/product/batch-generate-narration-audio',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  generateHallNarrationAudio: async (
    data: ShowroomHallNarrationGenerateReqVO
  ): Promise<ShowroomHallNarrationGenerateRespVO> => {
    return await request.post({
      url: '/showroom/hall/generate-narration-audio',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  batchGenerateHallNarrationAudio: async (): Promise<ShowroomHallNarrationBatchGenerateRespVO> => {
    return await request.post({
      url: '/showroom/hall/batch-generate-narration-audio',
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  getProductBatchGenerateNarrationAudioState: async (): Promise<ShowroomProductBatchGenerateStateRespVO> => {
    return await request.get({ url: '/showroom/product/batch-generate-narration-audio-state' })
  },
  startBatchGenerateNarrationScriptTask: async (
    data: ShowroomProductBatchGenerateReqVO
  ): Promise<ShowroomProductNarrationScriptTaskRespVO> => {
    return await request.post({
      url: '/showroom/product/batch-generate-narration-script/start',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  getBatchGenerateNarrationScriptTaskStatus: async (): Promise<ShowroomProductNarrationScriptTaskRespVO> => {
    return await request.get({ url: '/showroom/product/batch-generate-narration-script/status' })
  },
  generateProductCoverImage: async (
    data: ShowroomProductCoverGenerateReqVO
  ): Promise<ShowroomProductCoverGenerateRespVO> => {
    return await request.post({
      url: '/showroom/product/generate-cover-image',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  batchGenerateProductCoverImage: async (
    data: ShowroomProductBatchGenerateReqVO
  ): Promise<ShowroomProductBatchGenerateRespVO> => {
    return await request.post({
      url: '/showroom/product/batch-generate-cover-image',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  getProductBatchGenerateCoverImageState: async (): Promise<ShowroomProductCoverBatchTaskStateRespVO> => {
    return await request.get({ url: '/showroom/product/batch-generate-cover-image-state' })
  },
  startBatchTranslatePublishTask: async (
    data: ShowroomProductBatchGenerateReqVO
  ): Promise<ShowroomProductTranslatePublishBatchTaskRespVO> => {
    return await request.post({
      url: '/showroom/product/batch-translate-publish/start',
      data,
      timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT
    })
  },
  getBatchTranslatePublishTaskStatus: async (): Promise<ShowroomProductTranslatePublishBatchTaskRespVO> => {
    return await request.get({ url: '/showroom/product/batch-translate-publish/status' })
  },
  getHallPage: async (params: ShowroomPageQuery) => {
    return await request.get({ url: '/showroom/hall/page', params })
  },
  getHallProductOptions: async (): Promise<ShowroomHallProductOptionRespVO[]> => {
    return await request.get({ url: '/showroom/hall/product-options' })
  },
  getHallItemOptions: async (): Promise<any[]> => {
    return await request.get({ url: '/showroom/hall/item-options' })
  },
  createHall: async (data: any) => {
    return await request.post({ url: '/showroom/hall/create', data })
  },
  updateHall: async (data: any) => {
    return await request.put({ url: '/showroom/hall/update', data })
  },
  deleteHall: async (id: number) => {
    return await request.delete({ url: `/showroom/hall/delete?id=${id}` })
  },
  updateHallProductMapping: async (data: ShowroomHallMappingReqVO) => {
    return await request.put({ url: '/showroom/hall/update-item-mapping', data })
  },
  updateHallCanvasLayout: async (data: ShowroomHallCanvasLayoutReqVO) => {
    return await request.put({ url: '/showroom/hall/update-item-canvas-layout', data })
  },
  calculateHallBuCanvasLayout: async (data: ShowroomHallCanvasLayoutReqVO) => {
    return await request.post({ url: '/showroom/hall/calculate-bu-canvas-layout', data })
  },
  updateHallCanvasBackground: async (data: ShowroomHallCanvasBackgroundReqVO) => {
    return await request.put({ url: '/showroom/hall/update-canvas-background', data })
  },
  publishHallPreviewAsset: async (
    data: ShowroomHallPreviewAssetPublishReqVO
  ): Promise<ShowroomHallPreviewAssetPublishRespVO> => {
    return await request.post({ url: '/showroom/hall/publish-preview-asset', data })
  },
  exportHallConfigPackage: async () => {
    return await request.download({
      url: '/showroom/hall/config-package/export',
      ignoreErrorMessage: true
    })
  },
  importHallConfigPackage: async (
    data: FormData
  ): Promise<ShowroomHallConfigPackageImportRespVO> => {
    return await request.upload({
      url: '/showroom/hall/config-package/import',
      data,
      ignoreErrorMessage: true
    })
  },
  getApprovalPage: async (params: PageParam) => {
    return await request.get({ url: '/showroom/approval/page', params })
  },
  getApproval: async (id: number) => {
    return await request.get({ url: `/showroom/approval/get?id=${id}` })
  },
  supervisorApprove: async (data: ShowroomApprovalActionReqVO) => {
    return await request.post({ url: '/showroom/approval/supervisor-approve', data })
  },
  supervisorReject: async (data: ShowroomApprovalRejectReqVO) => {
    return await request.post({ url: '/showroom/approval/supervisor-reject', data })
  },
  gaoxinApprove: async (data: ShowroomApprovalActionReqVO) => {
    return await request.post({ url: '/showroom/approval/gaoxin-approve', data })
  },
  gaoxinReject: async (data: ShowroomApprovalRejectReqVO) => {
    return await request.post({ url: '/showroom/approval/gaoxin-reject', data })
  },
  createAssignment: async (data: any) => {
    return await request.post({ url: '/showroom/assignment/create', data })
  },
  completeAssignmentAndSubmit: async (data: any) => {
    return await request.post({ url: '/showroom/assignment/complete-and-submit', data })
  },
  getProductCommentPage: async (params: any) => {
    return await request.get({ url: '/showroom/product-comment/page', params })
  },
  createProductComment: async (data: any) => {
    return await request.post({ url: '/showroom/product-comment/create', data })
  },
  generateNarrationScript: async (data: any) => {
    return await request.post({ url: '/showroom/narration/generate-script', data })
  },
  generateNarrationAudio: async (data: any) => {
    return await request.post({ url: '/showroom/narration/generate-audio', data })
  },
  getNarration: async (params: {
    targetType: 'COMPANY' | 'HALL' | 'PRODUCT' | 'AWARD'
    targetId: number
    audienceType: 'PUBLIC'
    language: 'ZH' | 'EN'
  }) => {
    return await request.get({
      url: '/showroom/narration/get',
      params,
      ignoreErrorMessage: true
    })
  },
  getNarrationTtsDefaults: async (): Promise<ShowroomNarrationTtsDefaultsRespVO> => {
    return await request.get({ url: '/showroom/narration/tts-defaults' })
  },
  saveNarrationTtsDefaultVoice: async (data: { voice: string }): Promise<boolean> => {
    return await request.put({ url: '/showroom/narration/tts-default-voice', data })
  },
  saveNarrationTtsDefaultAppKey: async (data: { appKey: string }): Promise<boolean> => {
    return await request.put({ url: '/showroom/narration/tts-default-appkey', data })
  },
  saveNarrationTtsDefaultToken: async (data: { accessToken: string }): Promise<boolean> => {
    return await request.put({ url: '/showroom/narration/tts-default-token', data })
  },
  saveNarrationDraft: async (data: any) => {
    return await request.put({ url: '/showroom/narration/draft', data })
  },
  submitNarration: async (data: any) => {
    return await request.post({ url: '/showroom/narration/submit', data })
  },
  supervisorApproveNarration: async (data: any) => {
    return await request.post({ url: '/showroom/narration/supervisor-approve', data })
  },
  gaoxinApproveNarration: async (data: any) => {
    return await request.post({ url: '/showroom/narration/gaoxin-approve', data })
  },
  publishNarration: async (data: any) => {
    return await request.post({ url: '/showroom/narration/publish', data })
  }
}
