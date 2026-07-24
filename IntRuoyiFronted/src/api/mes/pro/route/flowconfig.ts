import request from '@/config/axios'
import type {
  EdhrProcessFormCandidateSourceType,
  EdhrProcessFormPermissionRuleRespVO
} from '@/api/mes/pro/edhr/processFormPermissionRule'

export type ProRouteFlowConfigType = 'SCHEDULE' | 'BATCH'
export type ProRouteFlowExecutionMode = 'SEQUENTIAL' | 'PARALLEL'
export type ProRouteFlowFormSlotType =
  | 'MAIN'
  | 'PROCESS_INSPECTION'
  | 'LOSS_REPORT'
  | 'PARAMETER_RECORD'
export type ProRouteFlowRecordCategory = 'BATCH_RECORD' | 'INTERNAL_RECORD'
export type ProRouteFlowValidationProfile = 'CONTROLLED_BATCH' | 'INTERNAL_TRACE'
export type ProRouteFlowRequiredPolicy =
  | 'REQUIRED'
  | 'CONDITIONAL_REQUIRED'
  | 'OPTIONAL'
  | 'SKIPPABLE_CONTROLLED'
export type ProRouteFlowOwnerRoleKey = 'PRODUCTION' | 'QUALITY' | 'EQUIPMENT' | 'QA' | 'ARCHIVE'
export type ProRouteFlowArchiveVisibility =
  | 'FINAL_DHR'
  | 'INTERNAL_REVIEW'
  | 'AUDIT_ONLY'
  | 'ATTACHMENT_REFERENCE'

export interface ProRouteFlowFormBindingVO {
  formBindingKey?: string | null
  formSlotType?: ProRouteFlowFormSlotType | null
  formTemplateId: number
  formTemplateName?: string | null
  formTemplateNameSnapshot?: string | null
  lastPublishedTemplateVersionId?: number | null
  lastPublishedTemplateVersionNo?: string | null
  instanceScope?: 'PROCESS' | 'BATCH_SHARED' | string
  sharedFormKey?: string | null
  fillableScopeJson?: string | null
  recordCategory?: ProRouteFlowRecordCategory | null
  validationProfile?: ProRouteFlowValidationProfile | null
  recordbookEnabled?: boolean | null
  requiredPolicy?: ProRouteFlowRequiredPolicy | null
  requiredConditionJson?: string | null
  ownerRoleKey?: ProRouteFlowOwnerRoleKey | null
  archiveVisibility?: ProRouteFlowArchiveVisibility | null
  permissionScopeId?: number | null
  slotConfigSnapshotHash?: string | null
  permissionRule?: EdhrProcessFormPermissionRuleRespVO | null
  candidateSourceType?: EdhrProcessFormCandidateSourceType | null
  candidateSourceIds?: number[]
  candidateSourceNames?: string[]
  reportSort: number
  remark?: string | null
}

export interface ProRouteFlowBatchRecordVO {
  batchRecordReportId: string
  batchRecordReportCode?: string | null
  batchRecordReportName?: string | null
  batchRecordDefinitionId?: number | null
  batchRecordVersionId?: number | null
  formSlotType?: ProRouteFlowFormSlotType | null
  instanceScope?: 'PROCESS' | 'BATCH_SHARED' | string
  sharedFormKey?: string | null
  fillableScopeJson?: string | null
  recordCategory?: ProRouteFlowRecordCategory | null
  validationProfile?: ProRouteFlowValidationProfile | null
  recordbookEnabled?: boolean | null
  permissionScopeId?: number | null
  requiredPolicy?: ProRouteFlowRequiredPolicy | null
  requiredConditionJson?: string | null
  ownerRoleKey?: ProRouteFlowOwnerRoleKey | null
  archiveVisibility?: ProRouteFlowArchiveVisibility | null
  slotConfigSnapshotHash?: string | null
  reportSort?: number | null
  remark?: string | null
}

export interface ProRouteFlowFormBindingSaveVO {
  formBindingKey?: string | null
  formSlotType?: ProRouteFlowFormSlotType | null
  formTemplateId: number
  formTemplateName?: string | null
  instanceScope?: 'PROCESS' | 'BATCH_SHARED' | string
  sharedFormKey?: string | null
  fillableScopeJson?: string | null
  recordCategory?: ProRouteFlowRecordCategory | null
  validationProfile?: ProRouteFlowValidationProfile | null
  recordbookEnabled?: boolean | null
  requiredPolicy?: ProRouteFlowRequiredPolicy | null
  requiredConditionJson?: string | null
  ownerRoleKey?: ProRouteFlowOwnerRoleKey | null
  archiveVisibility?: ProRouteFlowArchiveVisibility | null
  permissionScopeId?: number | null
  candidateSourceType?: EdhrProcessFormCandidateSourceType | null
  candidateSourceIds?: number[]
  candidateSourceNames?: string[]
  reportSort: number
  remark?: string | null
}

export interface ProRouteFlowProcessConfigVO {
  routeProcessId: number
  sort: number
  processCode: string
  processName: string
  useType: ProRouteFlowConfigType
  enabled: boolean
  routeConfigEnabled?: boolean | null
  keyFlag?: boolean | null
  executionMode?: ProRouteFlowExecutionMode | null
  productionQuantityFactor?: number | null
  batchRecordReports?: ProRouteFlowBatchRecordVO[]
  formBindings?: ProRouteFlowFormBindingVO[]
  routeScheduleConfigId?: number | null
  scheduleConfigVersion?: string | null
  capacityMode?: 'RESOURCE_CALCULATED' | 'MANUAL_OVERRIDE' | 'FINITE_HOURLY' | 'INFINITE_FORMULA' | null
  processHourlyCapacityTotal?: number
  hourlyCapacity?: number | null
  shiftHours?: number
  standardShiftCapacity?: number
  formulaQuantityFactorHours?: number | null
  formulaBaseHours?: number | null
  nightShiftEnabled?: boolean | null
  calendarRuleId?: number | null
  remark?: string | null
}

export interface ProRouteFlowProcessConfigSaveVO {
  routeProcessId: number
  enabled: boolean
  executionMode?: ProRouteFlowExecutionMode | null
  productionQuantityFactor?: number | null
  batchRecordReports?: ProRouteFlowBatchRecordVO[]
  formBindings?: ProRouteFlowFormBindingSaveVO[]
  remark?: string | null
}

export interface ProRouteFlowConfigSaveVO {
  routeId: number
  routeVersionId: number
  useType?: ProRouteFlowConfigType
  configVersion?: string | null
  remark?: string | null
  processConfigs: ProRouteFlowProcessConfigSaveVO[]
}

export const ProRouteFlowConfigApi = {
  getProcessConfigList: async (routeId: number, useType: ProRouteFlowConfigType, routeVersionId?: number) => {
    return await request.get<ProRouteFlowProcessConfigVO[]>({
      url: '/mes/pro/route/flow-config',
      params: { routeId, useType, routeVersionId }
    })
  },

  saveScheduleConfig: async (data: ProRouteFlowConfigSaveVO) => {
    return await request.post({
      url: '/mes/pro/route/flow-config/schedule/save',
      data
    })
  },

  saveBatchRecordConfig: async (data: ProRouteFlowConfigSaveVO) => {
    return await request.post({
      url: '/mes/pro/route/flow-config/batch-record/save',
      data
    })
  }
}
