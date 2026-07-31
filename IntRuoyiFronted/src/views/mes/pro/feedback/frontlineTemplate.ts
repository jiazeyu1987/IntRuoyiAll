import {
  FRONTLINE_FIELD_CODES,
  FRONTLINE_TEMPLATE_CODES,
  type FrontlineTemplateCode,
  type FrontlineTemplatePayloadReqVO,
  type FrontlineTemplateResolveReqVO
} from '@/api/mes/pro/feedbackFrontlineTemplate'

export interface FrontlineTemplateDraft {
  fieldValues: Record<string, any>
}

export type FrontlineTemplateContext = FrontlineTemplateResolveReqVO & {
  workOrderId?: number
  routeId?: number
}

export const PRODUCTION_SIMPLIFIED_FIELD_CODES = [
  'PREVIOUS_PROCESS_INPUT_QUANTITY',
  'DEVICE',
  'DEVICE_PARAMETERS',
  'OUTPUT_QUANTITY',
  'SCRAP_QUANTITY'
] as const

export const PQC_SIMPLIFIED_FIELD_CODES = ['PQC_RESULT'] as const

export const FRONTLINE_TEMPLATE_ALLOWED_FIELDS: Record<FrontlineTemplateCode, readonly string[]> = {
  [FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED]: PRODUCTION_SIMPLIFIED_FIELD_CODES,
  [FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED]: PQC_SIMPLIFIED_FIELD_CODES
}

const getAllowedFields = (templateCode: FrontlineTemplateCode | string | undefined) => {
  const allowedFields = FRONTLINE_TEMPLATE_ALLOWED_FIELDS[templateCode as FrontlineTemplateCode]
  if (!allowedFields) {
    throw new Error(`Unsupported frontline template code: ${String(templateCode ?? '')}`)
  }
  return allowedFields
}

export const resolveFrontlineContextKey = (context: FrontlineTemplateContext) =>
  [
    context.actualEmployeeId ?? '',
    context.routeProcessId ?? '',
    context.processId ?? '',
    context.templateCode ?? ''
  ].join('|')

export const resetFrontlineTemplateDraftForContext = (
  previousKey: string | undefined,
  nextKey: string,
  draft: FrontlineTemplateDraft
) => {
  if (previousKey === nextKey) {
    return false
  }
  for (const fieldCode of Object.keys(draft.fieldValues)) {
    delete draft.fieldValues[fieldCode]
  }
  return true
}

export const buildAllowedFieldValues = (
  templateCode: FrontlineTemplateCode | string | undefined,
  fieldValues: Record<string, any>
) => {
  const allowedFields = getAllowedFields(templateCode)
  const payloadValues: Record<string, any> = {}
  for (const fieldCode of allowedFields) {
    if (Object.prototype.hasOwnProperty.call(fieldValues, fieldCode)) {
      payloadValues[fieldCode] = fieldValues[fieldCode]
    }
  }
  return payloadValues
}

export const buildFrontlineTemplatePayload = (
  context: FrontlineTemplateContext,
  fieldValues: Record<string, any>
): FrontlineTemplatePayloadReqVO => ({
  workOrderId: context.workOrderId,
  routeId: context.routeId,
  processId: context.processId,
  routeProcessId: context.routeProcessId,
  actualEmployeeId: context.actualEmployeeId,
  templateCode: context.templateCode,
  fieldValues: buildAllowedFieldValues(context.templateCode, fieldValues)
})

export const createFrontlineDefaultValues = (templateCode: FrontlineTemplateCode | string | undefined) => {
  if (templateCode === FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED) {
    return {
      [FRONTLINE_FIELD_CODES.PQC_RESULT]: undefined
    }
  }
  if (templateCode === FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED) {
    return Object.fromEntries(
      PRODUCTION_SIMPLIFIED_FIELD_CODES.map((fieldCode) => [fieldCode, undefined])
    )
  }
  throw new Error(`Unsupported frontline template code: ${String(templateCode ?? '')}`)
}
