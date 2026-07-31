import request from '@/config/axios'

export const FRONTLINE_TEMPLATE_CODES = {
  PRODUCTION_SIMPLIFIED: 'PRODUCTION_SIMPLIFIED',
  PQC_SIMPLIFIED: 'PQC_SIMPLIFIED'
} as const

export const FRONTLINE_FIELD_CODES = {
  PREVIOUS_PROCESS_INPUT_QUANTITY: 'PREVIOUS_PROCESS_INPUT_QUANTITY',
  DEVICE: 'DEVICE',
  DEVICE_PARAMETERS: 'DEVICE_PARAMETERS',
  OUTPUT_QUANTITY: 'OUTPUT_QUANTITY',
  SCRAP_QUANTITY: 'SCRAP_QUANTITY',
  PQC_RESULT: 'PQC_RESULT'
} as const

export const FRONTLINE_PQC_RESULTS = {
  DETECTION_SUCCESS: 'DETECTION_SUCCESS',
  DETECTION_FAILED: 'DETECTION_FAILED'
} as const

export type FrontlineTemplateCode =
  (typeof FRONTLINE_TEMPLATE_CODES)[keyof typeof FRONTLINE_TEMPLATE_CODES]

export type FrontlineFieldCode = (typeof FRONTLINE_FIELD_CODES)[keyof typeof FRONTLINE_FIELD_CODES]

export interface FrontlineTemplateFieldVO {
  code: FrontlineFieldCode | string
  name: string
  valueType: string
  required: boolean
  options: string[]
}

export interface FrontlineTemplateDefinitionVO {
  code: FrontlineTemplateCode | string
  name: string
  type: string
  editableSubmitTime: boolean
  fields: FrontlineTemplateFieldVO[]
}

export interface FrontlineTemplateResolveReqVO {
  actualEmployeeId?: number
  routeProcessId?: number
  processId?: number
  templateCode?: FrontlineTemplateCode | string
}

export interface FrontlineTemplatePayloadReqVO extends FrontlineTemplateResolveReqVO {
  workOrderId?: number
  routeId?: number
  fieldValues: Record<string, any>
}

export interface FrontlineTemplatePayloadVO extends Required<FrontlineTemplatePayloadReqVO> {}

export const FrontlineTemplateApi = {
  getCatalog: async () => {
    return await request.get<FrontlineTemplateDefinitionVO[]>({
      url: '/mes/pro/feedback/frontline-template/catalog'
    })
  },
  resolveTemplate: async (params: FrontlineTemplateResolveReqVO) => {
    return await request.get<FrontlineTemplateDefinitionVO>({
      url: '/mes/pro/feedback/frontline-template/resolve',
      params
    })
  },
  validatePayload: async (data: FrontlineTemplatePayloadReqVO) => {
    return await request.post<FrontlineTemplatePayloadVO>({
      url: '/mes/pro/feedback/frontline-template/payload/validate',
      data
    })
  }
}
