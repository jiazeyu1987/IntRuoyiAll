import request from '@/config/axios'

export interface SrmProcurementContractPaymentVO {
  id?: number
  paymentStage: string
  paymentRatio: number
  paymentAmount: number
  dueDate: string
  paymentRemark?: string
}

export interface SrmProcurementContractSigningVO {
  id?: number
  signingParty: string
  signerName: string
  signingDate: string
  signingRemark?: string
}

export interface SrmProcurementContractAttachmentVO {
  id?: number
  attachmentName: string
  attachmentUrl: string
  attachmentType: string
}

export interface SrmProcurementContractVO {
  id: number
  contractNo: string
  contractTitle: string
  sourceType: string
  sourceTypeLabel?: string
  sourceId: number
  sourceNo: string
  supplierId: number
  supplierName: string
  contractAmount: number
  currency: string
  effectiveDate: string
  expireDate: string
  contractStatus: string
  contractStatusLabel?: string
  createdName?: string
  createdTime?: string
  cancelledName?: string
  cancelledTime?: string
  cancelReason?: string
  payments: SrmProcurementContractPaymentVO[]
  signings: SrmProcurementContractSigningVO[]
  attachments: SrmProcurementContractAttachmentVO[]
}

export interface SrmProcurementContractPageReqVO extends PageParam {
  contractNo?: string
  contractTitle?: string
  sourceType?: string
  supplierId?: number
  contractStatus?: string
}

export interface SrmProcurementContractSaveReqVO {
  sourceType: string
  sourceId: number
  contractTitle: string
  contractAmount: number
  currency: string
  effectiveDate: string
  expireDate: string
  payments: SrmProcurementContractPaymentVO[]
  signings: SrmProcurementContractSigningVO[]
  attachments: SrmProcurementContractAttachmentVO[]
}

export interface SrmProcurementContractCancelReqVO {
  id: number
  cancelReason: string
}

export const srmProcurementContractSourceTypeOptions = [
  { label: '非招标项目', value: 'NON_BIDDING' },
  { label: '招标项目', value: 'TENDER' }
] as const

export const srmProcurementContractStatusOptions = [
  { label: '生效中', value: 'EFFECTIVE' },
  { label: '已作废', value: 'CANCELLED' }
] as const

export const SrmProcurementContractApi = {
  getContractPage: async (params: SrmProcurementContractPageReqVO) => {
    return await request.get<PageResult<SrmProcurementContractVO[]>>({
      url: '/srm/procurement-contract/page',
      params
    })
  },

  getContract: async (id: number) => {
    return await request.get<SrmProcurementContractVO>({
      url: '/srm/procurement-contract/get',
      params: { id }
    })
  },

  createContract: async (data: SrmProcurementContractSaveReqVO) => {
    return await request.post<SrmProcurementContractVO>({
      url: '/srm/procurement-contract/create',
      data
    })
  },

  cancelContract: async (data: SrmProcurementContractCancelReqVO) => {
    return await request.put<boolean>({
      url: '/srm/procurement-contract/cancel',
      data
    })
  },

  deleteContract: async (id: number) => {
    return await request.delete<boolean>({
      url: '/srm/procurement-contract/delete',
      params: { id }
    })
  }
}
