import request from '@/config/axios'

export interface LossReason {
  id: number | string
  reasonCode: string
  reasonName: string
  createTime: string
}
export const listLossReasons = (keyword?: string) =>
  request.get<LossReason[]>({ url: '/mes/pro/loss-reason/list', params: { keyword } })
export const createLossReason = (reasonName: string) =>
  request.post<LossReason>({ url: '/mes/pro/loss-reason/create', data: { reasonName } })
export const importLossReasons = async (data: FormData) =>
  (await request.upload<{ data: number }>({ url: '/mes/pro/loss-reason/import', data })).data
export const downloadLossReasonTemplate = () =>
  request.download({ url: '/mes/pro/loss-reason/import-template' })
export const exportLossReasons = (keyword?: string) =>
  request.download({ url: '/mes/pro/loss-reason/export', params: { keyword } })
