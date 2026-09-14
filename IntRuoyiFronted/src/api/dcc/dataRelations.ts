import request from '@/config/axios'

export interface DccDataRelationCreateReqVO {
  productCatalogId: number | string
  projectCodeId: number | string
  registrationCertificateId: number | string
  relationRemark?: string | null
}

export interface DccDataRelationRespVO {
  id: number
  productCatalogId: number
  projectCodeId: number
  registrationCertificateId: number
  relationStatus: 'CONFIRMED'
  relationSource: 'MANUAL'
  relationRemark?: string | null
  confirmedBy?: number | null
  confirmedTime?: string | null
  createTime?: string | null
}

export const createDccDataRelation = async (
  data: DccDataRelationCreateReqVO
): Promise<DccDataRelationRespVO> => {
  return await request.post({ url: '/dcc/data-relations/create', data })
}

export const getDccDataRelationsByProductCatalogId = async (
  id: number | string
): Promise<DccDataRelationRespVO[]> => {
  return await request.get({ url: `/dcc/data-relations/by-product-catalog/${id}` })
}

export const getDccDataRelationsByProjectCodeId = async (
  id: number | string
): Promise<DccDataRelationRespVO[]> => {
  return await request.get({ url: `/dcc/data-relations/by-project-code/${id}` })
}

export const getDccDataRelationsByRegistrationCertificateId = async (
  id: number | string
): Promise<DccDataRelationRespVO[]> => {
  return await request.get({ url: `/dcc/data-relations/by-registration-certificate/${id}` })
}

export const deleteDccDataRelation = async (id: number | string): Promise<boolean> => {
  return await request.delete({ url: `/dcc/data-relations/${id}` })
}
