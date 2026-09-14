import request from '@/config/axios'

export interface EdhrRecordbookGlobalSettingRespVO {
  enabled: boolean
  configKey: string
  updatedBy?: string
  updatedAt?: string
}

export interface EdhrRecordbookGlobalSettingUpdateReqVO {
  enabled: boolean
}

export const getEdhrRecordbookGlobalSetting = async () => {
  return await request.get<EdhrRecordbookGlobalSettingRespVO>({
    url: '/mes/pro/edhr-recordbook-setting/global'
  })
}

export const updateEdhrRecordbookGlobalSetting = async (
  data: EdhrRecordbookGlobalSettingUpdateReqVO
) => {
  return await request.put<EdhrRecordbookGlobalSettingRespVO>({
    url: '/mes/pro/edhr-recordbook-setting/global',
    data
  })
}
