import request from '@/config/axios'
import type {
  QaInspectionRegulationPublishedVersionVO,
  QaInspectionRegulationSaveReqVO,
  QaCommonRegulationSetItemsUpgradeProcessReqVO
} from './index'
export type SimpleCommonDraft = Omit<QaInspectionRegulationSaveReqVO, 'processes'> & {
  processes: QaCommonRegulationSetItemsUpgradeProcessReqVO[]
}

const base = '/mes/qa/inspection-regulation/simple-common-parser'
export const SimpleCommonParserApi = {
  async parse(file: File): Promise<SimpleCommonDraft> {
    const data = new FormData()
    data.append('file', file)
    const result = await request.upload<{ data: SimpleCommonDraft }>({
      url: `${base}/parse`,
      data,
      timeout: 300000
    })
    return result.data
  },
  publish(data: SimpleCommonDraft): Promise<QaInspectionRegulationPublishedVersionVO> {
    return request.post({ url: `${base}/publish`, data })
  }
}
