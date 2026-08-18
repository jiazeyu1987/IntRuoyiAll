import request from '@/config/axios'

export interface DccRegistrationCertificateReminderConfigRespVO {
  id: number | string
  enabled: boolean
  dailyRunTime: string
  timezone: string
  thresholdDaysJson: string
  rowVersion: number
}

export interface DccRegistrationCertificateReminderConfigUpdateReqVO {
  enabled: boolean
  dailyRunTime: string
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
