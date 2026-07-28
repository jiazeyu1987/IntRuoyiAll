import request from '@/config/axios'

export interface EdhrReleaseDossierRequirementSettingRespVO {
  incomingInspectionReportRequired: boolean
  sterilizationReportRequired: boolean
  finishedProductInspectionReportRequired: boolean
  finishedProductInspectionRecordRequired: boolean
  configKey: string
  configHash: string
  updatedBy?: string
  updatedAt?: string
}

export interface EdhrReleaseDossierRequirementSettingUpdateReqVO {
  incomingInspectionReportRequired: boolean
  sterilizationReportRequired: boolean
  finishedProductInspectionReportRequired: boolean
  finishedProductInspectionRecordRequired: boolean
}

const URL = '/mes/pro/edhr-release-setting/dossier-requirements'

export const getEdhrReleaseDossierRequirementSetting = async () => {
  return await request.get<EdhrReleaseDossierRequirementSettingRespVO>({
    url: URL
  })
}

export const updateEdhrReleaseDossierRequirementSetting = async (
  data: EdhrReleaseDossierRequirementSettingUpdateReqVO
) => {
  return await request.put<EdhrReleaseDossierRequirementSettingRespVO>({
    url: URL,
    data
  })
}
