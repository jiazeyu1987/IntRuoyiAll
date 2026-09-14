import request from '@/config/axios'

export interface DccRegistrationCertificateReminderConfigRespVO {
  id: number | string
  enabled: boolean
  dailyRunTime: string
  timezone: string
  thresholdDaysJson: string
  thresholdRecipientUserIds: RegistrationCertificateThresholdRecipientUserIds
  rowVersion: number
}

export interface RegistrationCertificateThresholdRecipientUserIds {
  T_30: number[]
  T_8: number[]
  T_2: number[]
  T_1: number[]
}

export interface DccRegistrationCertificateReminderConfigUpdateReqVO {
  enabled: boolean
  dailyRunTime: string
  thresholdRecipientUserIds: RegistrationCertificateThresholdRecipientUserIds
  expectedRowVersion: number
}

const REMINDER_CONFIG_URL = '/dcc/registration-certificates/reminder-config'

export const getRegistrationCertificateReminderConfig = async () => {
  return await request.get<DccRegistrationCertificateReminderConfigRespVO>({
    url: REMINDER_CONFIG_URL
  })
}

export const updateRegistrationCertificateReminderConfig = async (
  data: DccRegistrationCertificateReminderConfigUpdateReqVO
) => {
  return await request.put<DccRegistrationCertificateReminderConfigRespVO>({
    url: REMINDER_CONFIG_URL,
    data
  })
}
