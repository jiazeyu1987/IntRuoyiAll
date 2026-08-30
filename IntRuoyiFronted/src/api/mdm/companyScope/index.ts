import request from '@/config/axios'

export const MDM_COMPANY_SCOPE_TYPE_USER = 'USER'
export const MDM_COMPANY_SCOPE_TYPE_ROLE = 'ROLE'
export const MDM_COMPANY_SCOPE_STATUS_ENABLE = 'ENABLE'
export const MDM_COMPANY_SCOPE_STATUS_DISABLE = 'DISABLE'

export interface MdmCompanyScopePageReqVO extends PageParam {
  scopeType?: string
  companyId?: number
  status?: string
  keyword?: string
}

export interface MdmCompanyScopeRespVO {
  id: number
  scopeType: string
  principalId: number
  principalName: string
  principalCode: string
  companyId: number
  companyCode: string
  companyName: string
  status: string
  revision?: number
  updateTime?: string
}

export const getCompanyScopePage = async (
  params: MdmCompanyScopePageReqVO
): Promise<PageResult<MdmCompanyScopeRespVO[]>> => {
  return await request.get({ url: '/mdm/company-scope/page', params })
}
