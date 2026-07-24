import request from '@/config/axios'

export const MDM_PRODUCT_STATUS_ENABLE = 'ENABLE'
export const MDM_PRODUCT_STATUS_DISABLE = 'DISABLE'

export interface MdmProductPageReqVO extends PageParam {
  keyword?: string
  productCode?: string
  dccProductCode?: string
  status?: string
}

export interface MdmProductRespVO {
  id: number
  productCode: string
  dccProductCode?: string | null
  nameCn: string
  nameEn?: string | null
  modelSpecification?: string | null
  category?: string | null
  status: string
  createTime?: string
  updateTime?: string
}

export interface MdmProductSimpleRespVO {
  id: number
  productCode: string
  dccProductCode?: string | null
  nameCn: string
  nameEn?: string | null
  modelSpecification?: string | null
  category?: string | null
  status: string
}

export interface MdmProductSaveReqVO {
  id?: number
  productCode: string
  dccProductCode?: string | null
  nameCn: string
  nameEn?: string | null
  modelSpecification?: string | null
  category?: string | null
  status: string
}

export interface MdmProductImportRowRespVO {
  rowNo: number
  productCode?: string | null
  dccProductCode?: string | null
  nameCn?: string | null
  nameEn?: string | null
  modelSpecification?: string | null
  category?: string | null
  currentStatus?: string | null
  importAction: string
  failureReason?: string | null
}

export interface MdmProductImportPreviewRespVO {
  batchId: number
  status: string
  totalCount: number
  createCount: number
  updateCount: number
  disableCount: number
  unchangedCount: number
  failureCount: number
  rows: MdmProductImportRowRespVO[]
}

export interface MdmProductReferenceRespVO {
  productId: number
  dccReferenceCount: number
  showroomReferenceCount: number
}

export const getProductPage = async (
  params: MdmProductPageReqVO
): Promise<PageResult<MdmProductRespVO[]>> => {
  return await request.get({ url: '/mdm/product/page', params })
}

export const getProduct = async (id: number): Promise<MdmProductRespVO> => {
  return await request.get({ url: '/mdm/product/get', params: { id } })
}

export const createProduct = async (data: MdmProductSaveReqVO): Promise<number> => {
  return await request.post({ url: '/mdm/product/create', data })
}

export const updateProduct = async (data: MdmProductSaveReqVO): Promise<boolean> => {
  return await request.put({ url: '/mdm/product/update', data })
}

export const updateProductStatus = async (id: number, status: string): Promise<boolean> => {
  return await request.put({ url: '/mdm/product/update-status', params: { id, status } })
}

export const getProductSimpleList = async (params?: {
  status?: string
  requireDccProductCode?: boolean
  keyword?: string
}): Promise<MdmProductSimpleRespVO[]> => {
  return await request.get({ url: '/mdm/product/simple-list', params })
}

export const exportProductExcel = async (params: MdmProductPageReqVO) => {
  return await request.download({ url: '/mdm/product/export-excel', params })
}

export const getImportTemplate = async () => {
  return await request.download({ url: '/mdm/product/get-import-template' })
}

export const importPreview = async (file: File): Promise<MdmProductImportPreviewRespVO> => {
  const data = new FormData()
  data.append('file', file)
  return await request.upload({ url: '/mdm/product/import-preview', data })
}

export const importConfirm = async (batchId: number): Promise<MdmProductImportPreviewRespVO> => {
  return await request.post({ url: '/mdm/product/import-confirm', data: { batchId } })
}

export const getProductReferences = async (productId: number): Promise<MdmProductReferenceRespVO> => {
  return await request.get({ url: '/mdm/product/references', params: { id: productId } })
}
