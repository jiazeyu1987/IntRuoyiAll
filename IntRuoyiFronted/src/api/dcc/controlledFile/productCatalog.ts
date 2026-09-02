import request from '@/config/axios'

export interface DccProductCatalogPageReqVO extends PageParam {
  keyword?: string
  categoryLevel1?: string
  categoryLevel2?: string
  productSequence?: string
  product?: string
  productStatus?: string
  dataSource?: string
  productCode?: string
  projectName?: string
  projectCode?: string
  registrationCertificateName?: string
  registrationCertificateNumber?: string
  certificateHolder?: string
  registrationPlace?: string
  effectiveDate?: string
  expiryDate?: string
  classification?: string
  registrationInfoLink?: string
  remark?: string
  projectCodeNotBlank?: boolean
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}

export interface DccProductCatalogRespVO {
  id: number
  dataSource: string
  categoryLevel1?: string | null
  categoryLevel2?: string | null
  productSequence?: string | null
  product?: string | null
  productCode?: string | null
  projectName?: string | null
  projectCode?: string | null
  batchRecordTotalRecognitionJson?: string | null
  registrationCertificateName?: string | null
  registrationCertificateNumber?: string | null
  certificateHolder?: string | null
  registrationPlace?: string | null
  effectiveDate?: string | null
  expiryDate?: string | null
  classification?: string | null
  registrationInfoLink?: string | null
  productStatus?: string | null
  remark?: string | null
  originalRowNo: number
}

export interface DccProductCatalogTreeNode extends DccProductCatalogRespVO {
  treeNodeId: string
  nodeType: 'categoryLevel1' | 'categoryLevel2' | 'product' | 'detail'
  treeLevel: number
  treeLabel: string
  children?: DccProductCatalogTreeNode[]
}

export interface DccProductCatalogSaveReqVO {
  dataSource: string
  categoryLevel1?: string | null
  categoryLevel2?: string | null
  productSequence?: string | null
  product: string
  productCode?: string | null
  projectName?: string | null
  projectCode?: string | null
  registrationCertificateName?: string | null
  registrationCertificateNumber?: string | null
  certificateHolder?: string | null
  registrationPlace?: string | null
  effectiveDate?: string | null
  expiryDate?: string | null
  classification?: string | null
  registrationInfoLink?: string | null
  productStatus?: string | null
  remark?: string | null
}

export interface DccProductCatalogUpdateReqVO extends DccProductCatalogSaveReqVO {
  originalRowNo: number
}

export const getProductCatalogPage = async (
  params: DccProductCatalogPageReqVO
): Promise<PageResult<DccProductCatalogRespVO[]>> => {
  return await request.get({ url: '/dcc/product-catalog/page', params })
}

export const createProductCatalog = async (
  data: DccProductCatalogSaveReqVO
): Promise<DccProductCatalogRespVO> => {
  return await request.post({ url: '/dcc/product-catalog/create', data })
}

export const updateProductCatalog = async (
  data: DccProductCatalogUpdateReqVO
): Promise<boolean> => {
  return await request.put({ url: '/dcc/product-catalog/update', data })
}

export const deleteProductCatalog = async (
  dataSource: string,
  originalRowNo: number
): Promise<boolean> => {
  return await request.delete({
    url: '/dcc/product-catalog/delete',
    params: { dataSource, originalRowNo }
  })
}
