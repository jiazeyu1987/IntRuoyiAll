import request from '@/config/axios'

export interface FenbeitongVoucherConfigVO {
  accountBookNumber: string
  voucherGroupNumber: string
  voucherGroupNo?: string
  templateErpFid: string
  currencyNumbers: Record<string, string>
  categoryAccountNumbers: Record<string, string>
  departmentDetailField: string
  employeeDetailField?: string
  creditAccountNumber: string
  creditDetailNumbers: Record<string, string>
  exchangeRateTypeNumber: string
  exchangeRate: number
  splitDeductibleTax: boolean
  taxAccountNumber?: string
  mockFixedJson?: string
  mockVoucherDate?: string
  mockYear?: number
  mockPeriod?: number
  fenbeitongBaseUrl?: string
  fenbeitongAccessToken?: string
  fenbeitongReimbursementApplyState?: number
  fenbeitongReimbursementPageSize?: number
}

export interface FenbeitongVoucherProcessReqVO extends FenbeitongVoucherConfigVO {
  fixedJson: string
  voucherDate: string
  year: number
  period: number
}

export interface FenbeitongVoucherConfiguredProcessReqVO {
  fixedJson: string
  voucherDate: string
  year: number
  period: number
}

export interface FenbeitongVoucherPreviewVO {
  sourceId: string
  sourceCode: string
  marker: string
  idempotencyKey: string
  contentHash: string
  totalAmount: number
  deductibleTaxAmount: number
  payload: Record<string, any>
}

export interface FenbeitongVoucherSaveVO {
  erpFid: string
  erpNumber: string
  saved: boolean
}

export interface FenbeitongVoucherProcessVO {
  sourceSystem: string
  sourceType: string
  sourceId: string
  sourceCode: string
  idempotencyKey: string
  contentHash: string
  processStatus: number
  processStage: string
  marker: string
  erpFid?: string
  erpNumber?: string
  erpDocumentStatus?: string
  voucherPayload?: string
  errorCode?: number
  errorMessage?: string
  createTime?: string
  updateTime?: string
}

export const FenbeitongVoucherApi = {
  getConfig: async () => {
    return await request.get({ url: '/erp/fenbeitong-voucher/config/get' })
  },

  getMockTemplate: async () => {
    return await request.get({ url: '/erp/fenbeitong-voucher/config/mock-template' })
  },

  saveConfig: async (data: FenbeitongVoucherConfigVO) => {
    return await request.put({ url: '/erp/fenbeitong-voucher/config/save', data })
  },

  previewFixedJson: async (data: FenbeitongVoucherProcessReqVO) => {
    return await request.post({ url: '/erp/fenbeitong-voucher/preview-fixed-json', data })
  },

  saveFixedJson: async (data: FenbeitongVoucherProcessReqVO) => {
    return await request.post({ url: '/erp/fenbeitong-voucher/save-fixed-json', data })
  },

  prepareFixedJson: async (data: FenbeitongVoucherProcessReqVO) => {
    return await request.post({ url: '/erp/fenbeitong-voucher/prepare-fixed-json', data })
  },

  previewConfiguredFixedJson: async (data: FenbeitongVoucherConfiguredProcessReqVO) => {
    return await request.post({ url: '/erp/fenbeitong-voucher/preview-configured-fixed-json', data })
  },

  saveConfiguredFixedJson: async (data: FenbeitongVoucherConfiguredProcessReqVO) => {
    return await request.post({ url: '/erp/fenbeitong-voucher/save-configured-fixed-json', data })
  },

  prepareConfiguredFixedJson: async (data: FenbeitongVoucherConfiguredProcessReqVO) => {
    return await request.post({
      url: '/erp/fenbeitong-voucher/prepare-configured-fixed-json',
      data
    })
  },

  getProcessBySourceId: async (sourceId: string) => {
    return await request.get({ url: '/erp/fenbeitong-voucher/process/get', params: { sourceId } })
  },

  viewErpVoucher: async (erpFid: string) => {
    return await request.get({ url: '/erp/fenbeitong-voucher/erp-view', params: { erpFid } })
  },

  queryErpVoucherBusinessInfo: async () => {
    return await request.get({ url: '/erp/fenbeitong-voucher/erp-business-info' })
  }
}
