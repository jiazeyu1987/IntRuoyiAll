import request from '@/config/axios'

export interface DccProductCatalogPageReqVO extends PageParam {
  keyword?: string
  categoryLevel1?: string
  categoryLevel2?: string
  productStatus?: string
  dataSource?: string
}

export interface DccProductCatalogRespVO {
  dataSource: string
  categoryLevel1?: string | null
  categoryLevel2?: string | null
  productSequence?: string | null
  product?: string | null
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
  originalRowNo: number
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
