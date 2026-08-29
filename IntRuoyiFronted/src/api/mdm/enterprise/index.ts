import request from '@/config/axios'

export const MDM_ENTERPRISE_TYPE_OWNED_COMPANY = 'OWNED_COMPANY'
export const MDM_ENTERPRISE_TYPE_ENTRUSTED_PARTY = 'ENTRUSTED_PARTY'
export const MDM_ENTERPRISE_STATUS_ENABLE = 'ENABLE'
export const MDM_ENTERPRISE_STATUS_DISABLE = 'DISABLE'

export interface MdmEnterprisePageReqVO extends PageParam {
  keyword?: string
  enterpriseCode?: string
  name?: string
  type?: string
  status?: string
}

export interface MdmEnterpriseRespVO {
  id: number
  enterpriseCode: string
  name: string
  type: string
  status: string
  revision?: number
  createTime?: string
  updateTime?: string
}

export interface MdmEnterpriseSimpleRespVO {
  id: number
  enterpriseCode: string
  name: string
  type: string
  status: string
  revision?: number
}

export interface MdmEnterpriseSaveReqVO {
  id?: number
  enterpriseCode: string
  name: string
  type: string
  status: string
}

export const getEnterprisePage = async (
  params: MdmEnterprisePageReqVO
): Promise<PageResult<MdmEnterpriseRespVO[]>> => {
  return await request.get({ url: '/mdm/enterprise/page', params })
}

export const getEnterprise = async (id: number): Promise<MdmEnterpriseRespVO> => {
  return await request.get({ url: '/mdm/enterprise/get', params: { id } })
}

export const createEnterprise = async (data: MdmEnterpriseSaveReqVO): Promise<number> => {
  return await request.post({ url: '/mdm/enterprise/create', data })
}

export const updateEnterprise = async (data: MdmEnterpriseSaveReqVO): Promise<boolean> => {
  return await request.put({ url: '/mdm/enterprise/update', data })
}

export const updateEnterpriseStatus = async (id: number, status: string): Promise<boolean> => {
  return await request.put({ url: '/mdm/enterprise/update-status', params: { id, status } })
}

export const deleteEnterprise = async (id: number): Promise<boolean> => {
  return await request.delete({ url: `/mdm/enterprise/delete?id=${id}` })
}

export const getEnterpriseSimpleList = async (params?: {
  type?: string
  status?: string
  keyword?: string
}): Promise<MdmEnterpriseSimpleRespVO[]> => {
  return await request.get({ url: '/mdm/enterprise/simple-list', params })
}
